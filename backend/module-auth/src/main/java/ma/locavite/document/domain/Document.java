package ma.locavite.document.domain;

import jakarta.persistence.*;
import ma.locavite.auth.domain.User;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private DocumentType type;

    @Column(name = "path", nullable = false, unique = true, length = 512)
    private String path;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DocumentStatus status = DocumentStatus.PENDING;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    public Document() {
    }

    public Document(Long id, User user, DocumentType type, String path, DocumentStatus status,
                    LocalDateTime uploadedAt, LocalDateTime verifiedAt, User verifiedBy) {
        this.id = id;
        this.user = user;
        this.type = type;
        this.path = path;
        this.status = status != null ? status : DocumentStatus.PENDING;
        this.uploadedAt = uploadedAt;
        this.verifiedAt = verifiedAt;
        this.verifiedBy = verifiedBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public User getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(User verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public static DocumentBuilder builder() {
        return new DocumentBuilder();
    }

    public static class DocumentBuilder {
        private Long id;
        private User user;
        private DocumentType type;
        private String path;
        private DocumentStatus status = DocumentStatus.PENDING;
        private LocalDateTime uploadedAt;
        private LocalDateTime verifiedAt;
        private User verifiedBy;

        public DocumentBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public DocumentBuilder user(User user) {
            this.user = user;
            return this;
        }

        public DocumentBuilder type(DocumentType type) {
            this.type = type;
            return this;
        }

        public DocumentBuilder path(String path) {
            this.path = path;
            return this;
        }

        public DocumentBuilder status(DocumentStatus status) {
            this.status = status;
            return this;
        }

        public DocumentBuilder uploadedAt(LocalDateTime uploadedAt) {
            this.uploadedAt = uploadedAt;
            return this;
        }

        public DocumentBuilder verifiedAt(LocalDateTime verifiedAt) {
            this.verifiedAt = verifiedAt;
            return this;
        }

        public DocumentBuilder verifiedBy(User verifiedBy) {
            this.verifiedBy = verifiedBy;
            return this;
        }

        public Document build() {
            return new Document(id, user, type, path, status, uploadedAt, verifiedAt, verifiedBy);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return Objects.equals(id, document.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
