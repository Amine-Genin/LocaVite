package ma.locavite.sale.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "visit_requests")
public class VisitRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_listing_id", nullable = false)
    private SaleListing saleListing;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "date_souhaitee", nullable = false)
    private LocalDate dateSouhaitee;

    @Column(name = "creneau_horaire", nullable = false, length = 50)
    private String creneauHoraire;

    @Column(nullable = false, length = 255)
    private String lieu;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_visite", nullable = false, length = 30)
    private VisitType typeVisite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VisitStatus statut = VisitStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "reminder_sent_at")
    private Instant reminderSentAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public VisitRequest() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SaleListing getSaleListing() {
        return saleListing;
    }

    public void setSaleListing(SaleListing saleListing) {
        this.saleListing = saleListing;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public LocalDate getDateSouhaitee() {
        return dateSouhaitee;
    }

    public void setDateSouhaitee(LocalDate dateSouhaitee) {
        this.dateSouhaitee = dateSouhaitee;
    }

    public String getCreneauHoraire() {
        return creneauHoraire;
    }

    public void setCreneauHoraire(String creneauHoraire) {
        this.creneauHoraire = creneauHoraire;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public VisitType getTypeVisite() {
        return typeVisite;
    }

    public void setTypeVisite(VisitType typeVisite) {
        this.typeVisite = typeVisite;
    }

    public VisitStatus getStatut() {
        return statut;
    }

    public void setStatut(VisitStatus statut) {
        this.statut = statut;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReminderSentAt() {
        return reminderSentAt;
    }

    public void setReminderSentAt(Instant reminderSentAt) {
        this.reminderSentAt = reminderSentAt;
    }
}
