package ma.locavite.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class RegisterRequest {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @NotBlank(message = "L'adresse email est obligatoire")
    @Email(
        regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
        message = "Format d'email invalide"
    )
    private String email;

    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    private String telephone;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
        message = "Le mot de passe doit contenir au moins 8 caractères, une majuscule et un chiffre"
    )
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

    // Record-like accessors
    public String nom() {
        return nom;
    }

    public String prenom() {
        return prenom;
    }

    public String email() {
        return email;
    }

    public String telephone() {
        return telephone;
    }

    public String password() {
        return password;
    }

    public static RegisterRequestBuilder builder() {
        return new RegisterRequestBuilder();
    }

    public static class RegisterRequestBuilder {
        private String nom;
        private String prenom;
        private String email;
        private String telephone;
        private String password;

        public RegisterRequestBuilder nom(String nom) {
            this.nom = nom;
            return this;
        }

        public RegisterRequestBuilder prenom(String prenom) {
            this.prenom = prenom;
            return this;
        }

        public RegisterRequestBuilder email(String email) {
            this.email = email;
            return this;
        }

        public RegisterRequestBuilder telephone(String telephone) {
            this.telephone = telephone;
            return this;
        }

        public RegisterRequestBuilder password(String password) {
            this.password = password;
            return this;
        }

        public RegisterRequest build() {
            return new RegisterRequest(nom, prenom, email, telephone, password);
        }
    }
}
