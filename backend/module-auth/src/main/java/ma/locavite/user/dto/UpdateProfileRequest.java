package ma.locavite.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Requête de mise à jour des informations de profil utilisateur")
public class UpdateProfileRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Schema(description = "Nom de famille", example = "Alaoui", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Schema(description = "Prénom", example = "Fatima", requiredMode = Schema.RequiredMode.REQUIRED)
    private String prenom;

    @NotBlank(message = "Le téléphone est obligatoire")
    @Pattern(regexp = "^(\\+212|0)[5-7]\\d{8}$", message = "Numéro de téléphone invalide")
    @Schema(description = "Numéro de téléphone marocain", example = "+212611223344", requiredMode = Schema.RequiredMode.REQUIRED)
    private String telephone;

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
}
