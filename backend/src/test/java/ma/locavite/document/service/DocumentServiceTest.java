package ma.locavite.document.service;

import ma.locavite.auth.domain.Role;
import ma.locavite.auth.domain.RoleType;
import ma.locavite.auth.domain.User;
import ma.locavite.document.domain.Document;
import ma.locavite.document.domain.DocumentStatus;
import ma.locavite.document.domain.DocumentType;
import ma.locavite.document.dto.DocumentResponse;
import ma.locavite.document.dto.VerifyDocumentRequest;
import ma.locavite.document.exception.DocumentNotFoundException;
import ma.locavite.document.exception.FileTooLargeException;
import ma.locavite.document.exception.InvalidDocumentException;
import ma.locavite.document.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private MinioService minioService;

    private DocumentService documentService;

    private User clientUser;
    private User otherUser;
    private User adminUser;

    private static final byte[] VALID_PDF_BYTES = "%PDF-1.4\n%LocaVite Test PDF\n%%EOF".getBytes(StandardCharsets.UTF_8);
    private static final byte[] EXE_BYTES = new byte[]{'M', 'Z', 0, 0, 1, 2, 3};

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(documentRepository, minioService);

        Role roleClient = Role.builder().id(1L).name(RoleType.CLIENT.name()).build();
        Role roleAdmin = Role.builder().id(2L).name(RoleType.ADMIN.name()).build();

        clientUser = User.builder()
                .id(10L)
                .email("client@locavite.ma")
                .nom("Alaoui")
                .prenom("Fatima")
                .roles(Set.of(roleClient))
                .build();

        otherUser = User.builder()
                .id(20L)
                .email("other@locavite.ma")
                .nom("Benali")
                .prenom("Karim")
                .roles(Set.of(roleClient))
                .build();

        adminUser = User.builder()
                .id(30L)
                .email("admin@locavite.ma")
                .nom("Admin")
                .prenom("Super")
                .roles(Set.of(roleAdmin))
                .build();
    }

    @Test
    @DisplayName("Upload PDF valide génère le bon chemin {userId}/{type}/{uuid}.{ext} et le statut PENDING")
    void testUploadPdfSuccess() {
        MockMultipartFile file = new MockMultipartFile("file", "permis.pdf", "application/pdf", VALID_PDF_BYTES);

        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            doc.setId(100L);
            return doc;
        });
        when(minioService.getPresignedUrl(anyString())).thenReturn("http://localhost:9000/presigned");

        DocumentResponse response = documentService.upload(clientUser, DocumentType.PERMIS, file);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getUserId()).isEqualTo(10L);
        assertThat(response.getType()).isEqualTo(DocumentType.PERMIS);
        assertThat(response.getStatus()).isEqualTo(DocumentStatus.PENDING);
        assertThat(response.getPath()).startsWith("10/PERMIS/").endsWith(".pdf");
        assertThat(response.getUrl()).isEqualTo("http://localhost:9000/presigned");

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(captor.capture());
        Document captured = captor.getValue();
        assertThat(captured.getUser()).isEqualTo(clientUser);
        assertThat(captured.getType()).isEqualTo(DocumentType.PERMIS);
        assertThat(captured.getStatus()).isEqualTo(DocumentStatus.PENDING);
        assertThat(captured.getPath()).matches("^10/PERMIS/[a-f0-9\\-]+\\.pdf$");

        verify(minioService).putObject(eq(captured.getPath()), any(), eq((long) VALID_PDF_BYTES.length), eq("application/pdf"));
    }

    @Test
    @DisplayName("Upload fichier > 5 Mo lève FileTooLargeException")
    void testUploadFileTooLargeThrows() {
        byte[] largeBytes = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "large.pdf", "application/pdf", largeBytes);

        assertThatThrownBy(() -> documentService.upload(clientUser, DocumentType.CIN, file))
                .isInstanceOf(FileTooLargeException.class)
                .hasMessageContaining("5 Mo");

        verifyNoInteractions(minioService);
        verifyNoInteractions(documentRepository);
    }

    @Test
    @DisplayName("Upload fichier exécutable (.exe) lève InvalidDocumentException")
    void testUploadExeThrows() {
        MockMultipartFile file = new MockMultipartFile("file", "test.exe", "application/octet-stream", EXE_BYTES);

        assertThatThrownBy(() -> documentService.upload(clientUser, DocumentType.PASSEPORT, file))
                .isInstanceOf(InvalidDocumentException.class)
                .hasMessageContaining("Type de fichier non autorisé");

        verifyNoInteractions(minioService);
        verifyNoInteractions(documentRepository);
    }

    @Test
    @DisplayName("Consultation par un autre utilisateur non admin lève AccessDeniedException")
    void testGetDocumentByOtherUserThrowsAccessDenied() {
        Document document = Document.builder()
                .id(1L)
                .user(clientUser)
                .type(DocumentType.CIN)
                .path("10/CIN/uuid.jpg")
                .status(DocumentStatus.PENDING)
                .build();

        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> documentService.getDocumentById(otherUser, 1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Accès refusé");
    }

    @Test
    @DisplayName("Consultation par le propriétaire retourne le document avec l'URL pré-signée")
    void testGetDocumentByOwnerSuccess() {
        Document document = Document.builder()
                .id(1L)
                .user(clientUser)
                .type(DocumentType.CIN)
                .path("10/CIN/uuid.jpg")
                .status(DocumentStatus.PENDING)
                .build();

        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(minioService.getPresignedUrl("10/CIN/uuid.jpg")).thenReturn("http://presigned-url");

        DocumentResponse response = documentService.getDocumentById(clientUser, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUrl()).isEqualTo("http://presigned-url");
    }

    @Test
    @DisplayName("Suppression par un autre utilisateur non admin lève AccessDeniedException")
    void testDeleteDocumentByOtherUserThrowsAccessDenied() {
        Document document = Document.builder()
                .id(1L)
                .user(clientUser)
                .type(DocumentType.PERMIS)
                .path("10/PERMIS/uuid.pdf")
                .status(DocumentStatus.PENDING)
                .build();

        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> documentService.deleteDocument(otherUser, 1L))
                .isInstanceOf(AccessDeniedException.class);

        verify(minioService, never()).deleteObject(anyString());
        verify(documentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Suppression par le propriétaire supprime dans MinIO et en base")
    void testDeleteDocumentByOwnerSuccess() {
        Document document = Document.builder()
                .id(1L)
                .user(clientUser)
                .type(DocumentType.PERMIS)
                .path("10/PERMIS/uuid.pdf")
                .status(DocumentStatus.PENDING)
                .build();

        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));

        documentService.deleteDocument(clientUser, 1L);

        verify(minioService).deleteObject("10/PERMIS/uuid.pdf");
        verify(documentRepository).delete(document);
    }

    @Test
    @DisplayName("Vérification admin met à jour le statut, verifiedAt et verifiedBy")
    void testVerifyDocumentSuccess() {
        Document document = Document.builder()
                .id(1L)
                .user(clientUser)
                .type(DocumentType.PERMIS)
                .path("10/PERMIS/uuid.pdf")
                .status(DocumentStatus.PENDING)
                .build();

        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(minioService.getPresignedUrl("10/PERMIS/uuid.pdf")).thenReturn("http://presigned-url");

        VerifyDocumentRequest request = new VerifyDocumentRequest(DocumentStatus.VALIDATED);
        DocumentResponse response = documentService.verifyDocument(1L, request, adminUser);

        assertThat(response.getStatus()).isEqualTo(DocumentStatus.VALIDATED);
        assertThat(response.getVerifiedBy()).isEqualTo(adminUser.getId());
        assertThat(response.getVerifiedAt()).isNotNull();
    }
}
