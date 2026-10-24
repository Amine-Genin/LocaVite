package ma.locavite.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.locavite.auth.domain.Role;
import ma.locavite.auth.domain.RoleType;
import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.RoleRepository;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.auth.service.EmailService;
import ma.locavite.auth.service.JwtService;
import ma.locavite.auth.service.RefreshTokenService;
import ma.locavite.document.domain.Document;
import ma.locavite.document.domain.DocumentStatus;
import ma.locavite.document.domain.DocumentType;
import ma.locavite.document.dto.VerifyDocumentRequest;
import ma.locavite.document.repository.DocumentRepository;
import ma.locavite.document.service.MinioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private MinioService minioService;

    @MockBean
    private EmailService emailService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    private User clientUser1;
    private User clientUser2;
    private User adminUser;

    private String client1Token;
    private String client2Token;
    private String adminToken;

    private static final byte[] VALID_PDF_BYTES = "%PDF-1.4\n%LocaVite Test PDF Content\n%%EOF".getBytes(StandardCharsets.UTF_8);
    private static final byte[] VALID_PNG_BYTES = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
    };
    private static final byte[] VALID_JPEG_BYTES = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10,
            0x4A, 0x46, 0x49, 0x46, 0x00, 0x01, 0x01, 0x01, 0x00, 0x60,
            0x00, 0x60, 0x00, 0x00, (byte) 0xFF, (byte) 0xD9
    };
    private static final byte[] EXE_BYTES = new byte[]{
            'M', 'Z', (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, (byte) 0xFF, (byte) 0xFF
    };

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        userRepository.deleteAll();

        Role roleAdmin = roleRepository.findByName(RoleType.ADMIN.name())
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.ADMIN.name()).build()));
        Role roleClient = roleRepository.findByName(RoleType.CLIENT.name())
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.CLIENT.name()).build()));

        clientUser1 = User.builder()
                .nom("Alaoui")
                .prenom("Fatima")
                .email("fatima.client1@locavite.ma")
                .telephone("+212611223344")
                .password(passwordEncoder.encode("Password123!"))
                .roles(Set.of(roleClient))
                .build();
        clientUser1 = userRepository.save(clientUser1);

        clientUser2 = User.builder()
                .nom("Benali")
                .prenom("Karim")
                .email("karim.client2@locavite.ma")
                .telephone("+212622334455")
                .password(passwordEncoder.encode("Password123!"))
                .roles(Set.of(roleClient))
                .build();
        clientUser2 = userRepository.save(clientUser2);

        adminUser = User.builder()
                .nom("AdminNom")
                .prenom("AdminPrenom")
                .email("admin@locavite.ma")
                .telephone("+212600112233")
                .password(passwordEncoder.encode("Password123!"))
                .roles(Set.of(roleAdmin))
                .build();
        adminUser = userRepository.save(adminUser);

        client1Token = jwtService.generateToken(clientUser1);
        client2Token = jwtService.generateToken(clientUser2);
        adminToken = jwtService.generateToken(adminUser);

        when(minioService.getPresignedUrl(anyString())).thenAnswer(invocation ->
                "http://localhost:9000/locavite-documents/" + invocation.getArgument(0) + "?token=mock-presigned-url");
        when(minioService.getPresignedUrl(anyString(), anyInt())).thenAnswer(invocation ->
                "http://localhost:9000/locavite-documents/" + invocation.getArgument(0) + "?token=mock-presigned-url");
        doNothing().when(minioService).putObject(anyString(), any(), any(Long.class), anyString());
        doNothing().when(minioService).deleteObject(anyString());
    }

    @Test
    @DisplayName("Upload PDF valide -> 201 Created avec métadonnées et URL pré-signée")
    void testUploadPdfValideReturns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "permis.pdf",
                "application/pdf",
                VALID_PDF_BYTES
        );

        mockMvc.perform(multipart("/api/users/me/documents")
                        .file(file)
                        .param("type", "PERMIS")
                        .header("Authorization", "Bearer " + client1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.userId", is(clientUser1.getId().intValue())))
                .andExpect(jsonPath("$.type", is("PERMIS")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.path", matchesPattern("^" + clientUser1.getId() + "/PERMIS/[a-f0-9\\-]+\\.pdf$")))
                .andExpect(jsonPath("$.url", containsString("http://localhost:9000/locavite-documents/")))
                .andExpect(jsonPath("$.uploadedAt", notNullValue()));
    }

    @Test
    @DisplayName("Upload fichier > 5 Mo -> 413 Payload Too Large")
    void testUploadFileGreaterThan5MoReturns413() throws Exception {
        byte[] oversizedBytes = new byte[5 * 1024 * 1024 + 1024]; // 5 Mo + 1 Ko
        System.arraycopy(VALID_PDF_BYTES, 0, oversizedBytes, 0, VALID_PDF_BYTES.length);

        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "gros_document.pdf",
                "application/pdf",
                oversizedBytes
        );

        mockMvc.perform(multipart("/api/users/me/documents")
                        .file(largeFile)
                        .param("type", "CIN")
                        .header("Authorization", "Bearer " + client1Token))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status", is(413)))
                .andExpect(jsonPath("$.message", containsString("5 Mo")));
    }

    @Test
    @DisplayName("Upload .exe (exécutable) -> 400 Bad Request")
    void testUploadExeReturns400() throws Exception {
        MockMultipartFile exeFile = new MockMultipartFile(
                "file",
                "malware.exe",
                "application/octet-stream",
                EXE_BYTES
        );

        mockMvc.perform(multipart("/api/users/me/documents")
                        .file(exeFile)
                        .param("type", "PASSEPORT")
                        .header("Authorization", "Bearer " + client1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Type de fichier non autorisé")));
    }

    @Test
    @DisplayName("Upload faux .pdf contenant en réalité un .exe -> 400 Bad Request via Tika")
    void testUploadDisguisedExeReturns400() throws Exception {
        MockMultipartFile disguisedExe = new MockMultipartFile(
                "file",
                "fake_document.pdf",
                "application/pdf",
                EXE_BYTES
        );

        mockMvc.perform(multipart("/api/users/me/documents")
                        .file(disguisedExe)
                        .param("type", "PERMIS")
                        .header("Authorization", "Bearer " + client1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("Type de fichier non autorisé")));
    }

    @Test
    @DisplayName("Consultation par un autre utilisateur -> 403 Forbidden")
    void testConsultationByOtherUserReturns403() throws Exception {
        // Document appartenant au clientUser1
        Document doc1 = Document.builder()
                .user(clientUser1)
                .type(DocumentType.CIN)
                .path(clientUser1.getId() + "/CIN/test-uuid.jpg")
                .status(DocumentStatus.PENDING)
                .build();
        doc1 = documentRepository.save(doc1);

        // clientUser2 tente de consulter le document de clientUser1
        mockMvc.perform(get("/api/users/me/documents/{id}", doc1.getId())
                        .header("Authorization", "Bearer " + client2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("Upload images valides JPEG et PNG -> 201 Created")
    void testUploadJpegAndPngReturns201() throws Exception {
        MockMultipartFile jpegFile = new MockMultipartFile(
                "file",
                "cin.jpeg",
                "image/jpeg",
                VALID_JPEG_BYTES
        );

        mockMvc.perform(multipart("/api/users/me/documents")
                        .file(jpegFile)
                        .param("type", "CIN")
                        .header("Authorization", "Bearer " + client1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type", is("CIN")))
                .andExpect(jsonPath("$.path", endsWith(".jpg")));

        MockMultipartFile pngFile = new MockMultipartFile(
                "file",
                "passeport.png",
                "image/png",
                VALID_PNG_BYTES
        );

        mockMvc.perform(multipart("/api/users/me/documents")
                        .file(pngFile)
                        .param("type", "PASSEPORT")
                        .header("Authorization", "Bearer " + client1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type", is("PASSEPORT")))
                .andExpect(jsonPath("$.path", endsWith(".png")));
    }

    @Test
    @DisplayName("Liste des documents de l'utilisateur connecté -> 200 OK avec URLs")
    void testGetUserDocumentsReturns200() throws Exception {
        Document doc1 = Document.builder()
                .user(clientUser1)
                .type(DocumentType.PERMIS)
                .path(clientUser1.getId() + "/PERMIS/uuid1.pdf")
                .status(DocumentStatus.PENDING)
                .build();
        Document doc2 = Document.builder()
                .user(clientUser1)
                .type(DocumentType.CIN)
                .path(clientUser1.getId() + "/CIN/uuid2.jpg")
                .status(DocumentStatus.VALIDATED)
                .build();
        documentRepository.save(doc1);
        documentRepository.save(doc2);

        mockMvc.perform(get("/api/users/me/documents")
                        .header("Authorization", "Bearer " + client1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].url", notNullValue()))
                .andExpect(jsonPath("$[1].url", notNullValue()));
    }

    @Test
    @DisplayName("Consultation de son propre document -> 200 OK")
    void testGetOwnDocumentReturns200() throws Exception {
        Document doc = Document.builder()
                .user(clientUser1)
                .type(DocumentType.PERMIS)
                .path(clientUser1.getId() + "/PERMIS/uuid1.pdf")
                .status(DocumentStatus.PENDING)
                .build();
        doc = documentRepository.save(doc);

        mockMvc.perform(get("/api/users/me/documents/{id}", doc.getId())
                        .header("Authorization", "Bearer " + client1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(doc.getId().intValue())))
                .andExpect(jsonPath("$.userId", is(clientUser1.getId().intValue())))
                .andExpect(jsonPath("$.url", notNullValue()));
    }

    @Test
    @DisplayName("Suppression de son propre document -> 204 No Content")
    void testDeleteOwnDocumentReturns204() throws Exception {
        Document doc = Document.builder()
                .user(clientUser1)
                .type(DocumentType.PERMIS)
                .path(clientUser1.getId() + "/PERMIS/uuid1.pdf")
                .status(DocumentStatus.PENDING)
                .build();
        doc = documentRepository.save(doc);

        mockMvc.perform(delete("/api/users/me/documents/{id}", doc.getId())
                        .header("Authorization", "Bearer " + client1Token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/me/documents/{id}", doc.getId())
                        .header("Authorization", "Bearer " + client1Token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Suppression d'un document par un autre utilisateur -> 403 Forbidden")
    void testDeleteDocumentByOtherUserReturns403() throws Exception {
        Document doc = Document.builder()
                .user(clientUser1)
                .type(DocumentType.PERMIS)
                .path(clientUser1.getId() + "/PERMIS/uuid1.pdf")
                .status(DocumentStatus.PENDING)
                .build();
        doc = documentRepository.save(doc);

        mockMvc.perform(delete("/api/users/me/documents/{id}", doc.getId())
                        .header("Authorization", "Bearer " + client2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Vérification admin : PUT /api/admin/documents/{id}/verify (VALIDATED) -> 200 OK")
    void testAdminVerifyDocumentValidatedReturns200() throws Exception {
        Document doc = Document.builder()
                .user(clientUser1)
                .type(DocumentType.PERMIS)
                .path(clientUser1.getId() + "/PERMIS/uuid1.pdf")
                .status(DocumentStatus.PENDING)
                .build();
        doc = documentRepository.save(doc);

        VerifyDocumentRequest request = new VerifyDocumentRequest(DocumentStatus.VALIDATED);

        mockMvc.perform(put("/api/admin/documents/{id}/verify", doc.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(doc.getId().intValue())))
                .andExpect(jsonPath("$.status", is("VALIDATED")))
                .andExpect(jsonPath("$.verifiedAt", notNullValue()))
                .andExpect(jsonPath("$.verifiedBy", is(adminUser.getId().intValue())));
    }

    @Test
    @DisplayName("Vérification admin : PUT /api/admin/documents/{id}/verify (REJECTED) -> 200 OK")
    void testAdminVerifyDocumentRejectedReturns200() throws Exception {
        Document doc = Document.builder()
                .user(clientUser1)
                .type(DocumentType.CIN)
                .path(clientUser1.getId() + "/CIN/uuid1.jpg")
                .status(DocumentStatus.PENDING)
                .build();
        doc = documentRepository.save(doc);

        VerifyDocumentRequest request = new VerifyDocumentRequest(DocumentStatus.REJECTED);

        mockMvc.perform(put("/api/admin/documents/{id}/verify", doc.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(doc.getId().intValue())))
                .andExpect(jsonPath("$.status", is("REJECTED")))
                .andExpect(jsonPath("$.verifiedAt", notNullValue()))
                .andExpect(jsonPath("$.verifiedBy", is(adminUser.getId().intValue())));
    }

    @Test
    @DisplayName("Vérification admin par un simple client -> 403 Forbidden")
    void testVerifyDocumentByClientReturns403() throws Exception {
        Document doc = Document.builder()
                .user(clientUser1)
                .type(DocumentType.CIN)
                .path(clientUser1.getId() + "/CIN/uuid1.jpg")
                .status(DocumentStatus.PENDING)
                .build();
        doc = documentRepository.save(doc);

        VerifyDocumentRequest request = new VerifyDocumentRequest(DocumentStatus.VALIDATED);

        mockMvc.perform(put("/api/admin/documents/{id}/verify", doc.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + client1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Document introuvable -> 404 Not Found")
    void testDocumentNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/users/me/documents/9999")
                        .header("Authorization", "Bearer " + client1Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }
}
