package ma.locavite.payment.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "refunds")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @Column(nullable = false, length = 255)
    private String raison;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RefundStatus statut = RefundStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RefundMethod methode = RefundMethod.MANUAL;

    @Column(name = "avoir_path", length = 512)
    private String avoirPath;

    @Column(name = "reference", length = 100)
    private String reference;

    @Column(name = "audit_trail", columnDefinition = "TEXT")
    private String auditTrail;

    @CreationTimestamp
    @Column(name = "date_operation", nullable = false, updatable = false)
    private Instant date;

    public Refund() {
    }

    public Refund(
            Long id,
            Long paymentId,
            BigDecimal montant,
            String raison,
            RefundStatus statut,
            RefundMethod methode,
            Instant date) {
        this.id = id;
        this.paymentId = paymentId;
        this.montant = montant;
        this.raison = raison;
        this.statut = statut != null ? statut : RefundStatus.PENDING;
        this.methode = methode != null ? methode : RefundMethod.MANUAL;
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public String getRaison() {
        return raison;
    }

    public void setRaison(String raison) {
        this.raison = raison;
    }

    public RefundStatus getStatut() {
        return statut;
    }

    public void setStatut(RefundStatus statut) {
        this.statut = statut;
    }

    public RefundMethod getMethode() {
        return methode;
    }

    public void setMethode(RefundMethod methode) {
        this.methode = methode;
    }

    public String getAvoirPath() {
        return avoirPath;
    }

    public void setAvoirPath(String avoirPath) {
        this.avoirPath = avoirPath;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getAuditTrail() {
        return auditTrail;
    }

    public void setAuditTrail(String auditTrail) {
        this.auditTrail = auditTrail;
    }

    public Instant getDate() {
        return date;
    }

    public void setDate(Instant date) {
        this.date = date;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Refund refund = (Refund) o;
        return id != null && Objects.equals(id, refund.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
