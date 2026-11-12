package ma.locavite.reservation.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Objects;

@Entity
@Table(name = "seasons")
public class Season {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String nom;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal coefficient;

    @Column(name = "priorite", nullable = false)
    private Integer priorite = 1;

    @Column(name = "agence_id")
    private Long agenceId;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(name = "is_recurring")
    private Boolean isRecurring = Boolean.FALSE;

    public Season() {
    }

    public Season(String nom, LocalDate dateDebut, LocalDate dateFin, BigDecimal coefficient) {
        this(null, nom, dateDebut, dateFin, coefficient, 1, null, null, false);
    }

    public Season(String nom, LocalDate dateDebut, LocalDate dateFin, BigDecimal coefficient, Integer priorite) {
        this(null, nom, dateDebut, dateFin, coefficient, priorite, null, null, false);
    }

    public Season(Long id, String nom, LocalDate dateDebut, LocalDate dateFin, BigDecimal coefficient,
                  Integer priorite, Long agenceId, Long vehicleId, Boolean isRecurring) {
        this.id = id;
        this.nom = nom;
        this.name = nom;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.coefficient = coefficient;
        this.priorite = priorite != null ? priorite : 1;
        this.agenceId = agenceId;
        this.vehicleId = vehicleId;
        this.isRecurring = isRecurring != null ? isRecurring : Boolean.FALSE;
    }

    @PrePersist
    @PreUpdate
    public void syncNames() {
        if (nom == null && name != null) {
            nom = name;
        } else if (name == null && nom != null) {
            name = nom;
        }
    }

    public boolean matchesDate(LocalDate date) {
        if (date == null || dateDebut == null || dateFin == null) {
            return false;
        }
        if (!date.isBefore(dateDebut) && !date.isAfter(dateFin)) {
            return true;
        }
        if (Boolean.TRUE.equals(isRecurring)) {
            MonthDay dayMd = MonthDay.from(date);
            MonthDay startMd = MonthDay.from(dateDebut);
            MonthDay endMd = MonthDay.from(dateFin);
            if (startMd.compareTo(endMd) <= 0) {
                return dayMd.compareTo(startMd) >= 0 && dayMd.compareTo(endMd) <= 0;
            } else {
                return dayMd.compareTo(startMd) >= 0 || dayMd.compareTo(endMd) <= 0;
            }
        }
        return false;
    }

    public boolean isVehicleOverride() {
        return vehicleId != null;
    }

    public boolean isAgencyOverride() {
        return agenceId != null && vehicleId == null;
    }

    public boolean isGlobal() {
        return agenceId == null && vehicleId == null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return (name != null) ? name : nom;
    }

    public void setName(String name) {
        this.name = name;
        if (this.nom == null) {
            this.nom = name;
        }
    }

    public String getNom() {
        return (nom != null) ? nom : name;
    }

    public void setNom(String nom) {
        this.nom = nom;
        if (this.name == null) {
            this.name = nom;
        }
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

    public BigDecimal getCoefficient() {
        return coefficient;
    }

    public void setCoefficient(BigDecimal coefficient) {
        this.coefficient = coefficient;
    }

    public Integer getPriorite() {
        return priorite != null ? priorite : 1;
    }

    public void setPriorite(Integer priorite) {
        this.priorite = priorite;
    }

    public Integer getPriority() {
        return getPriorite();
    }

    public void setPriority(Integer priority) {
        setPriorite(priority);
    }

    public Long getAgenceId() {
        return agenceId;
    }

    public void setAgenceId(Long agenceId) {
        this.agenceId = agenceId;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Boolean isRecurring() {
        return Boolean.TRUE.equals(isRecurring);
    }

    public Boolean getIsRecurring() {
        return isRecurring;
    }

    public void setIsRecurring(Boolean recurring) {
        this.isRecurring = recurring;
    }

    public void setRecurring(Boolean recurring) {
        this.isRecurring = recurring;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Season season = (Season) o;
        return Objects.equals(id, season.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
