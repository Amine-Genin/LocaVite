package ma.locavite.document.service;

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
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    public static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 Mo
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "application/pdf"
    );

    private final DocumentRepository documentRepository;
    private final MinioService minioService;
    private final Tika tika;

    public DocumentService(DocumentRepository documentRepository, MinioService minioService) {
        this.documentRepository = documentRepository;
        this.minioService = minioService;
        this.tika = new Tika();
    }

    @Transactional
    public DocumentResponse upload(User currentUser, DocumentType type, MultipartFile file) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentification requise pour uploader un document");
        }

        if (type == null) {
            throw new InvalidDocumentException("Le type de document est obligatoire (PERMIS, CIN, PASSEPORT)");
        }

        if (file == null || file.isEmpty()) {
            throw new InvalidDocumentException("Le fichier est requis et ne peut pas être vide");
        }

        // Vérification de la taille max (5 Mo)
        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("Fichier trop volumineux reçu : {} octets (max: {} octets)", file.getSize(), MAX_FILE_SIZE);
            throw new FileTooLargeException("Le fichier dépasse la taille maximale autorisée de 5 Mo");
        }

        // Validation MIME avec Apache Tika (détection magic bytes)
        String detectedMimeType;
        try {
            byte[] fileBytes = file.getBytes();
            detectedMimeType = tika.detect(fileBytes);
        } catch (IOException e) {
            log.error("Impossible de lire les données du fichier uploadé : {}", e.getMessage(), e);
            throw new InvalidDocumentException("Impossible de lire le contenu du fichier");
        }

        if (detectedMimeType == null || !ALLOWED_MIME_TYPES.contains(detectedMimeType.toLowerCase())) {
            log.warn("Type de fichier non autorisé : detectedMimeType={}", detectedMimeType);
            throw new InvalidDocumentException("Type de fichier non autorisé : " + detectedMimeType + ". Types acceptés : JPEG, PNG, PDF");
        }

        String extension = resolveExtension(detectedMimeType);
        String filename = UUID.randomUUID() + "." + extension;
        // Format chemin : {userId}/{type}/{uuid}.{ext}
        String path = String.format("%d/%s/%s", currentUser.getId(), type.name(), filename);

        // Upload vers MinIO
        try {
            minioService.putObject(path, file.getInputStream(), file.getSize(), detectedMimeType);
        } catch (IOException e) {
            log.error("Erreur d'entrée/sortie lors de l'upload vers MinIO : {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la lecture du fichier pour stockage MinIO", e);
        }

        // Enregistrement des métadonnées en base
        Document document = Document.builder()
                .user(currentUser)
                .type(type)
                .path(path)
                .status(DocumentStatus.PENDING)
                .build();

        Document savedDocument = documentRepository.save(document);
        log.info("Document sauvegardé avec succès [id={}, userId={}, type={}, path={}]",
                savedDocument.getId(), currentUser.getId(), type, path);

        String presignedUrl = minioService.getPresignedUrl(path);
        return DocumentResponse.fromEntity(savedDocument, presignedUrl);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getUserDocuments(User currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentification requise");
        }

        List<Document> documents = documentRepository.findByUserIdOrderByUploadedAtDesc(currentUser.getId());
        return documents.stream()
                .map(doc -> DocumentResponse.fromEntity(doc, minioService.getPresignedUrl(doc.getPath())))
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(User currentUser, Long documentId) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentification requise");
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document non trouvé avec l'id : " + documentId));

        boolean isOwner = document.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = hasAdminRole(currentUser);

        if (!isOwner && !isAdmin) {
            log.warn("Accès non autorisé au document [id={}] par l'utilisateur [id={}]", documentId, currentUser.getId());
            throw new AccessDeniedException("Accès refusé : vous n'avez pas l'autorisation de consulter ce document");
        }

        String presignedUrl = minioService.getPresignedUrl(document.getPath());
        return DocumentResponse.fromEntity(document, presignedUrl);
    }

    @Transactional
    public void deleteDocument(User currentUser, Long documentId) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentification requise");
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document non trouvé avec l'id : " + documentId));

        boolean isOwner = document.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = hasAdminRole(currentUser);

        if (!isOwner && !isAdmin) {
            log.warn("Tentative de suppression non autorisée du document [id={}] par l'utilisateur [id={}]", documentId, currentUser.getId());
            throw new AccessDeniedException("Accès refusé : vous n'avez pas l'autorisation de supprimer ce document");
        }

        // Suppression dans MinIO
        minioService.deleteObject(document.getPath());

        // Suppression en base
        documentRepository.delete(document);
        log.info("Document supprimé avec succès [id={}, path={}]", documentId, document.getPath());
    }

    @Transactional
    public DocumentResponse verifyDocument(Long documentId, VerifyDocumentRequest request, User adminUser) {
        if (request == null || request.getStatus() == null || request.getStatus() == DocumentStatus.PENDING) {
            throw new IllegalArgumentException("Le statut de vérification doit être VALIDATED ou REJECTED");
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document non trouvé avec l'id : " + documentId));

        document.setStatus(request.getStatus());
        document.setVerifiedAt(LocalDateTime.now());
        document.setVerifiedBy(adminUser);

        Document savedDocument = documentRepository.save(document);
        log.info("Document [id={}] vérifié avec succès par l'administrateur [id={}] : nouveau statut={}",
                documentId, adminUser != null ? adminUser.getId() : null, request.getStatus());

        String presignedUrl = minioService.getPresignedUrl(savedDocument.getPath());
        return DocumentResponse.fromEntity(savedDocument, presignedUrl);
    }

    private boolean hasAdminRole(User user) {
        if (user.getRoles() == null) {
            return false;
        }
        return user.getRoles().stream()
                .anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getName()) || "ROLE_ADMIN".equalsIgnoreCase(r.getName()));
    }

    private String resolveExtension(String mimeType) {
        return switch (mimeType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "application/pdf" -> "pdf";
            default -> "bin";
        };
    }
}
