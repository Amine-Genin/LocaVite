package ma.locavite.sale.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "vehicle_history")
public class VehicleHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_listing_id", nullable = false)
    private SaleListing saleListing;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private HistoryEventType typeEvenement;

    @Column(name = "event_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer kilometrage;

    @Column(name = "document_url", length = 512)
    private String documentUrl;

    @Column(nullable = false)
    private boolean verified = false;

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

    public HistoryEventType getTypeEvenement() {
        return typeEvenement;
    }

    public void setTypeEvenement(HistoryEventType typeEvenement) {
        this.typeEvenement = typeEvenement;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getKilometrage() {
        return kilometrage;
    }

    public void setKilometrage(Integer kilometrage) {
        this.kilometrage = kilometrage;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}
