package ma.locavite.sale.domain;

import jakarta.persistence.*;
import ma.locavite.vehicle.domain.Vehicle;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "sale_listings")
public class SaleListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "agence_id", nullable = false)
    private Long agenceId;

    @Column(nullable = false, length = 255)
    private String titre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "prix_vente", nullable = false, precision = 12, scale = 2)
    private BigDecimal prixVente;

    @Column(name = "prix_nego", nullable = false)
    private Boolean prixNego = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehicleCondition etat;

    @Column
    private Integer kilometrage;

    @Column(name = "premier_proprietaire", nullable = false)
    private Boolean premierProprietaire = true;

    @Column(name = "date_mise_en_circulation")
    private LocalDate dateMiseEnCirculation;

    @Column(name = "numero_chassis", length = 50)
    private String numeroChassis;

    @Column(name = "nombre_proprietaires")
    private Integer nombreProprietaires = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SaleStatus statut = SaleStatus.DRAFT;

    @Column(nullable = false)
    private Boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "sold_at")
    private Instant soldAt;

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

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public Long getAgenceId() {
        return agenceId;
    }

    public void setAgenceId(Long agenceId) {
        this.agenceId = agenceId;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrixVente() {
        return prixVente;
    }

    public void setPrixVente(BigDecimal prixVente) {
        this.prixVente = prixVente;
    }

    public Boolean getPrixNego() {
        return prixNego;
    }

    public void setPrixNego(Boolean prixNego) {
        this.prixNego = prixNego;
    }

    public VehicleCondition getEtat() {
        return etat;
    }

    public void setEtat(VehicleCondition etat) {
        this.etat = etat;
    }

    public Integer getKilometrage() {
        return kilometrage;
    }

    public void setKilometrage(Integer kilometrage) {
        this.kilometrage = kilometrage;
    }

    public Boolean getPremierProprietaire() {
        return premierProprietaire;
    }

    public void setPremierProprietaire(Boolean premierProprietaire) {
        this.premierProprietaire = premierProprietaire;
    }

    public LocalDate getDateMiseEnCirculation() {
        return dateMiseEnCirculation;
    }

    public void setDateMiseEnCirculation(LocalDate dateMiseEnCirculation) {
        this.dateMiseEnCirculation = dateMiseEnCirculation;
    }

    public String getNumeroChassis() {
        return numeroChassis;
    }

    public void setNumeroChassis(String numeroChassis) {
        this.numeroChassis = numeroChassis;
    }

    public Integer getNombreProprietaires() {
        return nombreProprietaires;
    }

    public void setNombreProprietaires(Integer nombreProprietaires) {
        this.nombreProprietaires = nombreProprietaires;
    }

    public SaleStatus getStatut() {
        return statut;
    }

    public void setStatut(SaleStatus statut) {
        this.statut = statut;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
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

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getSoldAt() {
        return soldAt;
    }

    public void setSoldAt(Instant soldAt) {
        this.soldAt = soldAt;
    }
}
