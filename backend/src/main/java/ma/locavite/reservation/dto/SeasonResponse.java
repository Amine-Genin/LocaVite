package ma.locavite.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ma.locavite.reservation.domain.Season;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Représentation d'une saison tarifaire")
public class SeasonResponse {

    @Schema(description = "Identifiant unique de la saison", example = "1")
    private Long id;

    @Schema(description = "Nom de la saison", example = "Haute saison été")
    private String nom;

    @Schema(description = "Date de début", example = "2026-07-01")
    private LocalDate dateDebut;

    @Schema(description = "Date de fin", example = "2026-08-31")
    private LocalDate dateFin;

    @Schema(description = "Coefficient multiplicateur", example = "1.50")
    private BigDecimal coefficient;

    @Schema(description = "Niveau de priorité", example = "3")
    private Integer priorite;

    @Schema(description = "Identifiant de l'agence (override agence, si non nul)", example = "10")
    private Long agenceId;

    @Schema(description = "Identifiant du véhicule (override véhicule, si non nul)", example = "25")
    private Long vehicleId;

    @Schema(description = "Indique si la saison est récurrente", example = "true")
    private Boolean isRecurring;

    public SeasonResponse() {
    }

    public SeasonResponse(Long id, String nom, LocalDate dateDebut, LocalDate dateFin, BigDecimal coefficient,
                          Integer priorite, Long agenceId, Long vehicleId, Boolean isRecurring) {
        this.id = id;
        this.nom = nom;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.coefficient = coefficient;
        this.priorite = priorite;
        this.agenceId = agenceId;
        this.vehicleId = vehicleId;
        this.isRecurring = isRecurring;
    }

    public static SeasonResponse fromEntity(Season season) {
        if (season == null) {
            return null;
        }
        return new SeasonResponse(
                season.getId(),
                season.getNom(),
                season.getDateDebut(),
                season.getDateFin(),
                season.getCoefficient(),
                season.getPriorite(),
                season.getAgenceId(),
                season.getVehicleId(),
                season.isRecurring()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getName() {
        return nom;
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
        return priorite;
    }

    public void setPriorite(Integer priorite) {
        this.priorite = priorite;
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

    public Boolean getIsRecurring() {
        return isRecurring;
    }

    public void setIsRecurring(Boolean recurring) {
        isRecurring = recurring;
    }
}
