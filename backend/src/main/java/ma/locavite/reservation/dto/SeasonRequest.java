package ma.locavite.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Requête de création ou mise à jour d'une saison tarifaire")
public class SeasonRequest {

    @NotBlank(message = "Le nom de la saison est obligatoire")
    @Schema(description = "Nom de la saison", example = "Haute saison été", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nom;

    @NotNull(message = "La date de début est obligatoire")
    @Schema(description = "Date de début de la saison", example = "2026-07-01", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    @Schema(description = "Date de fin de la saison", example = "2026-08-31", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate dateFin;

    @NotNull(message = "Le coefficient est obligatoire")
    @DecimalMin(value = "0.01", message = "Le coefficient doit être strictement positif")
    @Schema(description = "Coefficient multiplicateur du tarif", example = "1.50", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal coefficient;

    @Schema(description = "Priorité d'application de la saison (les priorités supérieures l'emportent)", example = "3")
    private Integer priorite = 1;

    @Schema(description = "Identifiant de l'agence pour un override agence (optionnel)", example = "10")
    private Long agenceId;

    @Schema(description = "Identifiant du véhicule pour un override véhicule (optionnel)", example = "25")
    private Long vehicleId;

    @Schema(description = "Indique si la saison est récurrente chaque année", example = "true")
    private Boolean isRecurring = Boolean.FALSE;

    public SeasonRequest() {
    }

    public SeasonRequest(String nom, LocalDate dateDebut, LocalDate dateFin, BigDecimal coefficient,
                         Integer priorite, Long agenceId, Long vehicleId, Boolean isRecurring) {
        this.nom = nom;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.coefficient = coefficient;
        this.priorite = priorite != null ? priorite : 1;
        this.agenceId = agenceId;
        this.vehicleId = vehicleId;
        this.isRecurring = isRecurring != null ? isRecurring : Boolean.FALSE;
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

    public void setName(String name) {
        this.nom = name;
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
