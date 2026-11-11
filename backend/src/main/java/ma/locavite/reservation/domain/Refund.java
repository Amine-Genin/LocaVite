package ma.locavite.reservation.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "refunds")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @Column(nullable = false, length = 255)
    private String raison;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundStatus statut = RefundStatus.PENDING;

    @CreationTimestamp
    @Column(name = "date_operation", nullable = false, updatable = false)
    private Instant date;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Reservation getReservation() { return reservation; }
    public void setReservation(Reservation reservation) { this.reservation = reservation; }
    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
    public String getRaison() { return raison; }
    public void setRaison(String raison) { this.raison = raison; }
    public RefundStatus getStatut() { return statut; }
    public void setStatut(RefundStatus statut) { this.statut = statut; }
    public Instant getDate() { return date; }
    public void setDate(Instant date) { this.date = date; }
}
