package ma.locavite.reservation.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "caution_transactions")
public class CautionTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CautionTransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CautionStatus statut;

    @CreationTimestamp
    @Column(name = "date_operation", nullable = false, updatable = false)
    private Instant date;

    @Column(length = 500)
    private String note;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Reservation getReservation() { return reservation; }
    public void setReservation(Reservation reservation) { this.reservation = reservation; }
    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
    public CautionTransactionType getType() { return type; }
    public void setType(CautionTransactionType type) { this.type = type; }
    public CautionStatus getStatut() { return statut; }
    public void setStatut(CautionStatus statut) { this.statut = statut; }
    public Instant getDate() { return date; }
    public void setDate(Instant date) { this.date = date; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
