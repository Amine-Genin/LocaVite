package ma.locavite.document.dto;

import ma.locavite.document.domain.Document;
import ma.locavite.document.domain.DocumentStatus;
import ma.locavite.document.domain.DocumentType;

import java.time.LocalDateTime;

public class DocumentResponse {

    private Long id;
    private Long userId;
    private DocumentType type;
    private String path;
    private DocumentStatus status;
    private String url;
    private LocalDateTime uploadedAt;
    private LocalDateTime verifiedAt;
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
