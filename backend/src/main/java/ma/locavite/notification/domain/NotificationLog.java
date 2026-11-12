package ma.locavite.notification.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "notification_logs")
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "recipient", nullable = false, length = 100)
    private String recipient;

    @Column(name = "destinataire", length = 100)
    private String destinataire;

    @Column(name = "channel", nullable = false, length = 30)
    private String channel;

    @Column(name = "canal", length = 30)
    private String canal;

    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    @Column(name = "type", length = 50)
    private String type;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "statut", length = 30)
    private String statut;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "attempts", nullable = false)
    private Integer attempts = 1;

    @Column(name = "tentatives", nullable = false)
    private Integer tentatives = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public NotificationLog() {
    }

    public NotificationLog(Long reservationId, Long userId, String recipient, String channel,
                           String notificationType, String content, String status,
                           String errorMessage, Integer attempts) {
        this.reservationId = reservationId;
        this.userId = userId;
        this.recipient = recipient;
        this.destinataire = recipient;
        this.channel = channel;
        this.canal = channel;
        this.notificationType = notificationType;
        this.type = notificationType;
        this.content = content;
        this.message = content;
        this.status = status;
        this.statut = status;
        this.errorMessage = errorMessage;
        this.attempts = attempts != null ? attempts : 1;
        this.tentatives = this.attempts;
    }

    @PrePersist
    @PreUpdate
    public void syncSynonyms() {
        if (destinataire == null && recipient != null) {
            destinataire = recipient;
        } else if (recipient == null && destinataire != null) {
            recipient = destinataire;
        }
        if (canal == null && channel != null) {
            canal = channel;
        } else if (channel == null && canal != null) {
            channel = canal;
        }
        if (type == null && notificationType != null) {
            type = notificationType;
        } else if (notificationType == null && type != null) {
            notificationType = type;
        }
        if (message == null && content != null) {
            message = content;
        } else if (content == null && message != null) {
            content = message;
        }
        if (statut == null && status != null) {
            statut = status;
        } else if (status == null && statut != null) {
            status = statut;
        }
        if (tentatives == null && attempts != null) {
            tentatives = attempts;
        } else if (attempts == null && tentatives != null) {
            attempts = tentatives;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
        this.destinataire = recipient;
    }

    public String getDestinataire() {
        return destinataire != null ? destinataire : recipient;
    }

    public void setDestinataire(String destinataire) {
        this.destinataire = destinataire;
        this.recipient = destinataire;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
        this.canal = channel;
    }

    public String getCanal() {
        return canal != null ? canal : channel;
    }

    public void setCanal(String canal) {
        this.canal = canal;
        this.channel = canal;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
        this.type = notificationType;
    }

    public String getType() {
        return type != null ? type : notificationType;
    }

    public void setType(String type) {
        this.type = type;
        this.notificationType = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.message = content;
    }

    public String getMessage() {
        return message != null ? message : content;
    }

    public void setMessage(String message) {
        this.message = message;
        this.content = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.statut = status;
    }

    public String getStatut() {
        return statut != null ? statut : status;
    }

    public void setStatut(String statut) {
        this.statut = statut;
        this.status = statut;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
        this.tentatives = attempts;
    }

    public Integer getTentatives() {
        return tentatives != null ? tentatives : attempts;
    }

    public void setTentatives(Integer tentatives) {
        this.tentatives = tentatives;
        this.attempts = tentatives;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationLog that = (NotificationLog) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
