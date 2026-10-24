package ma.locavite.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ma.locavite.document.domain.Document;
import ma.locavite.document.domain.DocumentStatus;
import ma.locavite.document.domain.DocumentType;

import java.time.LocalDateTime;

@Schema(description = "Détails d'un document justificatif téléversé")
public class DocumentResponse {

    @Schema(description = "Identifiant unique du document", example = "10")
    private Long id;

    @Schema(description = "Identifiant de l'utilisateur propriétaire du document", example = "1")
    private Long userId;

    @Schema(description = "Type de document (CIN, PERMIS)", example = "PERMIS")
    private DocumentType type;

    @Schema(description = "Chemin de stockage dans le bucket MinIO", example = "1/PERMIS/550e8400-e29b-41d4-a716-446655440000.pdf")
    private String path;

    @Schema(description = "Statut actuel de vérification", example = "VALIDATED")
    private DocumentStatus status;

    @Schema(description = "URL présignée sécurisée pour télécharger/visualiser le document", example = "http://localhost:9000/locavite-documents/...")
    private String url;

    @Schema(description = "Date et heure de téléversement", example = "2026-10-20T10:15:30")
    private LocalDateTime uploadedAt;

    @Schema(description = "Date et heure de validation/rejet", example = "2026-10-20T11:00:00")
    private LocalDateTime verifiedAt;

    @Schema(description = "Identifiant de l'administrateur ayant vérifié le document", example = "2")
    private Long verifiedBy;

    public DocumentResponse() {
    }

    public DocumentResponse(Long id, Long userId, DocumentType type, String path, DocumentStatus status,
                            String url, LocalDateTime uploadedAt, LocalDateTime verifiedAt, Long verifiedBy) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.path = path;
        this.status = status;
        this.url = url;
        this.uploadedAt = uploadedAt;
        this.verifiedAt = verifiedAt;
        this.verifiedBy = verifiedBy;
    }

    public static DocumentResponse fromEntity(Document doc, String presignedUrl) {
        if (doc == null) {
            return null;
        }
        return new DocumentResponse(
                doc.getId(),
                doc.getUser() != null ? doc.getUser().getId() : null,
                doc.getType(),
                doc.getPath(),
                doc.getStatus(),
                presignedUrl,
                doc.getUploadedAt(),
                doc.getVerifiedAt(),
                doc.getVerifiedBy() != null ? doc.getVerifiedBy().getId() : null
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public DocumentType getType() {
        return type;
    }

    public void setType(DocumentType type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public Long getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(Long verifiedBy) {
        this.verifiedBy = verifiedBy;
    }
}
