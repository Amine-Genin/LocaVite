package ma.locavite.payment.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "invoices",
        indexes = {
                @Index(name = "idx_invoices_payment_id", columnList = "payment_id"),
                @Index(name = "idx_invoices_numero", columnList = "numero"),
                @Index(name = "idx_invoices_date_emission", columnList = "date_emission")
        }
)
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = false, unique = true, length = 30)
    private String numero;

    @Column(name = "payment_id", nullable = false, unique = true)
    private Long paymentId;

    @Column(name = "montant_ht", nullable = false, precision = 12, scale = 2)
    private BigDecimal montantHT;

    @Column(name = "montant_tva", nullable = false, precision = 12, scale = 2)
    private BigDecimal montantTVA;

    @Column(name = "montant_ttc", nullable = false, precision = 12, scale = 2)
    private BigDecimal montantTTC;

    @Column(name = "chemin_pdf", nullable = false, length = 512)
    private String cheminPdf;

    @Column(name = "date_emission", nullable = false)
    private Instant dateEmission;

    public Invoice() {
    }

    public Invoice(
            Long id,
            String numero,
            Long paymentId,
            BigDecimal montantHT,
            BigDecimal montantTVA,
            BigDecimal montantTTC,
            String cheminPdf,
            Instant dateEmission) {
        this.id = id;
        this.numero = numero;
        this.paymentId = paymentId;
        this.montantHT = montantHT;
        this.montantTVA = montantTVA;
        this.montantTTC = montantTTC;
        this.cheminPdf = cheminPdf;
        this.dateEmission = dateEmission;
    }

    @PrePersist
    protected void onCreate() {
        if (this.dateEmission == null) {
            this.dateEmission = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public BigDecimal getMontantHT() {
        return montantHT;
    }

    public void setMontantHT(BigDecimal montantHT) {
        this.montantHT = montantHT;
    }

    public BigDecimal getMontantTVA() {
        return montantTVA;
    }

    public void setMontantTVA(BigDecimal montantTVA) {
        this.montantTVA = montantTVA;
    }

    public BigDecimal getMontantTTC() {
        return montantTTC;
    }

    public void setMontantTTC(BigDecimal montantTTC) {
        this.montantTTC = montantTTC;
    }

    public String getCheminPdf() {
        return cheminPdf;
    }

    public void setCheminPdf(String cheminPdf) {
        this.cheminPdf = cheminPdf;
    }

    public Instant getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(Instant dateEmission) {
        this.dateEmission = dateEmission;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Invoice invoice = (Invoice) o;
        return Objects.equals(id, invoice.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
