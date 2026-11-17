package ma.locavite.payment.cmi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKey {

    @Id
    @Column(name = "\"key\"", length = 255)
    private String key;

    @Column(name = "response", nullable = false, columnDefinition = "TEXT")
    private String response;

    @Column(name = "status_code", nullable = false)
    private int statusCode = 200;

    @Column(name = "content_type", length = 100)
    private String contentType = "application/json";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public IdempotencyKey() {
    }

    public IdempotencyKey(String key, String response, int statusCode, String contentType) {
        this.key = key;
        this.response = response;
        this.statusCode = statusCode;
        this.contentType = contentType != null ? contentType : "application/json";
    }

    public boolean isExpired(Duration ttl) {
        if (createdAt == null || ttl == null) {
            return false;
        }
        return Instant.now().isAfter(createdAt.plus(ttl));
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdempotencyKey that = (IdempotencyKey) o;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
