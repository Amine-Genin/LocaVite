package ma.locavite.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Set;

@Schema(description = "Représentation publique d'un utilisateur sans mot de passe")
public class UserResponse {

    @Schema(description = "Identifiant unique de l'utilisateur", example = "1")
    private Long id;

    @Schema(description = "Nom de famille", example = "Alaoui")
    private String nom;

    @Schema(description = "Prénom", example = "Fatima")
    private String prenom;

    @Schema(description = "Adresse email", example = "fatima.alaoui@example.com")
    private String email;

    @Schema(description = "Numéro de téléphone", example = "+212611223344")
    private String telephone;

    @Schema(description = "Rôles attribués à l'utilisateur", example = "[\"CLIENT\", \"AGENCE\"]")
    private Set<String> roles;

    @Schema(description = "Date et heure d'inscription", example = "2026-10-17T14:30:00")
    private LocalDateTime createdAt;

    public UserResponse() {
    }

    public UserResponse(Long id, String nom, String prenom, String email, String telephone, Set<String> roles, LocalDateTime createdAt) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.telephone = telephone;
        this.roles = roles;
        this.createdAt = createdAt;
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

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Record-like accessors
    public Long id() {
        return id;
    }

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

    public Set<String> roles() {
        return roles;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public static UserResponseBuilder builder() {
        return new UserResponseBuilder();
    }

    public static class UserResponseBuilder {
        private Long id;
        private String nom;
        private String prenom;
        private String email;
        private String telephone;
        private Set<String> roles;
        private LocalDateTime createdAt;

        public UserResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserResponseBuilder nom(String nom) {
            this.nom = nom;
            return this;
        }

        public UserResponseBuilder prenom(String prenom) {
            this.prenom = prenom;
            return this;
        }

        public UserResponseBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserResponseBuilder telephone(String telephone) {
            this.telephone = telephone;
            return this;
        }

        public UserResponseBuilder roles(Set<String> roles) {
            this.roles = roles;
            return this;
        }

        public UserResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserResponse build() {
            return new UserResponse(id, nom, prenom, email, telephone, roles, createdAt);
        }
    }
}
