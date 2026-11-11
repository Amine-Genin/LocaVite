package ma.locavite.mail.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "email_logs")
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Column(name = "destinataire")
    private String destinataire;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "sujet")
    private String sujet;

    @Column(name = "template_name")
    private String templateName;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "statut", length = 30)
    private String statut;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "attempts", nullable = false)
    private Integer attempts = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public EmailLog() {
    }

    public EmailLog(Long reservationId, String recipient, String subject, String templateName,
                    String status, String errorMessage, Integer attempts) {
        this.reservationId = reservationId;
        this.recipient = recipient;
        this.destinataire = recipient;
        this.subject = subject;
        this.sujet = subject;
        this.templateName = templateName;
        this.status = status;
        this.statut = status;
        this.errorMessage = errorMessage;
        this.attempts = attempts != null ? attempts : 1;
    }

    @PrePersist
    @PreUpdate
    public void syncSynonyms() {
        if (destinataire == null && recipient != null) {
            destinataire = recipient;
        } else if (recipient == null && destinataire != null) {
            recipient = destinataire;
        }
        if (sujet == null && subject != null) {
            sujet = subject;
        } else if (subject == null && sujet != null) {
            subject = sujet;
        }
        if (statut == null && status != null) {
            statut = status;
        } else if (status == null && statut != null) {
            status = statut;
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

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
        this.sujet = subject;
    }

    public String getSujet() {
        return sujet != null ? sujet : subject;
    }

    public void setSujet(String sujet) {
        this.sujet = sujet;
        this.subject = sujet;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
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

    public String getErreur() {
        return errorMessage;
    }

    public void setErreur(String erreur) {
        this.errorMessage = erreur;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    public Integer getTentatives() {
        return attempts;
    }

    public void setTentatives(Integer tentatives) {
        this.attempts = tentatives;
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
        EmailLog emailLog = (EmailLog) o;
        return Objects.equals(id, emailLog.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
