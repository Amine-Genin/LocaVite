package ma.locavite.reservation.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "lieu_prise_en_charge", nullable = false, length = 255)
    private String lieuPriseEnCharge;

    @Column(name = "lieu_retour", nullable = false, length = 255)
    private String lieuRetour;

    @Column(name = "prix_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal prixTotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal caution;

    @Column(name = "prix_sous_total", precision = 12, scale = 2)
    private BigDecimal prixSousTotal;

    @Column(name = "prix_options", precision = 12, scale = 2)
    private BigDecimal prixOptions;

    @Column(name = "prix_remise", precision = 12, scale = 2)
    private BigDecimal prixRemise;

    @Column(name = "prix_tva", precision = 12, scale = 2)
    private BigDecimal prixTva;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus statut = ReservationStatus.PENDING;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reservation_options", joinColumns = @JoinColumn(name = "reservation_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "option_name", nullable = false, length = 50)
    private Set<ReservationOption> options = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Reservation() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public String getLieuPriseEnCharge() {
        return lieuPriseEnCharge;
    }

    public void setLieuPriseEnCharge(String lieuPriseEnCharge) {
        this.lieuPriseEnCharge = lieuPriseEnCharge;
    }

    public String getLieuRetour() {
        return lieuRetour;
    }

    public void setLieuRetour(String lieuRetour) {
        this.lieuRetour = lieuRetour;
    }

    public BigDecimal getPrixTotal() {
        return prixTotal;
    }

    public void setPrixTotal(BigDecimal prixTotal) {
        this.prixTotal = prixTotal;
    }

    public BigDecimal getCaution() {
        return caution;
    }

    public void setCaution(BigDecimal caution) {
        this.caution = caution;
    }

    public BigDecimal getPrixSousTotal() { return prixSousTotal; }
    public void setPrixSousTotal(BigDecimal prixSousTotal) { this.prixSousTotal = prixSousTotal; }
    public BigDecimal getPrixOptions() { return prixOptions; }
    public void setPrixOptions(BigDecimal prixOptions) { this.prixOptions = prixOptions; }
    public BigDecimal getPrixRemise() { return prixRemise; }
    public void setPrixRemise(BigDecimal prixRemise) { this.prixRemise = prixRemise; }
    public BigDecimal getPrixTva() { return prixTva; }
    public void setPrixTva(BigDecimal prixTva) { this.prixTva = prixTva; }

    public ReservationStatus getStatut() {
        return statut;
    }

    public void setStatut(ReservationStatus statut) {
        this.statut = statut;
    }

    public Set<ReservationOption> getOptions() {
        return options;
    }

    public void setOptions(Set<ReservationOption> options) {
        this.options = options != null ? options : new HashSet<>();
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
        if (!(o instanceof Reservation that)) return false;
        return Objects.equals(id, that.id) || (code != null && Objects.equals(code, that.code));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code);
    }
}
