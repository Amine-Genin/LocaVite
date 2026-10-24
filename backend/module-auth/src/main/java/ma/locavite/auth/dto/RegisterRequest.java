package ma.locavite.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Requête d'inscription d'un nouvel utilisateur")
public class RegisterRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Schema(description = "Nom de famille", example = "Alaoui", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Schema(description = "Prénom", example = "Fatima", requiredMode = Schema.RequiredMode.REQUIRED)
    private String prenom;

    @NotBlank(message = "L'adresse email est obligatoire")
    @Email(
        regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
        message = "Format d'email invalide"
    )
    @Schema(description = "Adresse email valide", example = "fatima.alaoui@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @Schema(description = "Numéro de téléphone marocain", example = "+212611223344", requiredMode = Schema.RequiredMode.REQUIRED)
    private String telephone;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
        message = "Le mot de passe doit contenir au moins 8 caractères, une majuscule et un chiffre"
    )
    @Schema(description = "Mot de passe sécurisé (au moins 8 caractères, 1 majuscule, 1 chiffre)", example = "Password123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    public RegisterRequest() {
    }

    public RegisterRequest(String nom, String prenom, String email, String telephone, String password) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.telephone = telephone;
        this.password = password;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String nom;
        private String prenom;
        private String email;
        private String telephone;
        private String password;

        public Builder nom(String nom) {
            this.nom = nom;
            return this;
        }

        public Builder prenom(String prenom) {
            this.prenom = prenom;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder telephone(String telephone) {
            this.telephone = telephone;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public RegisterRequest build() {
            return new RegisterRequest(nom, prenom, email, telephone, password);
        }
    }
}
