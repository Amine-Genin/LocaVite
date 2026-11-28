package ma.locavite.reservation.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "option_prices")
public class OptionPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_type", nullable = false, unique = true, length = 50)
    private ReservationOption type;

    @Column(name = "libelle", length = 120)
    private String libelle;

    @Column(name = "prix_jour", nullable = false, precision = 12, scale = 2)
    private BigDecimal prixJour;

    @Column(name = "actif", nullable = false)
    private Boolean actif = Boolean.TRUE;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ReservationOption getType() { return type; }
    public void setType(ReservationOption type) { this.type = type; }
    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }
    public BigDecimal getPrixJour() { return prixJour; }
    public void setPrixJour(BigDecimal prixJour) { this.prixJour = prixJour; }
    public Boolean getActif() { return actif != null ? actif : Boolean.TRUE; }
    public void setActif(Boolean actif) { this.actif = actif != null ? actif : Boolean.TRUE; }
}
