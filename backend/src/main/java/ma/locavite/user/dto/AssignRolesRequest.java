package ma.locavite.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

@Schema(description = "Requête d'attribution de rôles à un utilisateur par un administrateur")
public class AssignRolesRequest {

    @NotEmpty(message = "La liste des rôles ne peut pas être vide")
    @Schema(description = "Ensemble non vide de rôles valides (CLIENT, AGENCE, ADMIN)", example = "[\"CLIENT\", \"AGENCE\"]", requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<@NotBlank(message = "Le nom du rôle ne peut pas être vide") String> roles;

    public AssignRolesRequest() {
    }

    public AssignRolesRequest(Set<String> roles) {
        this.roles = roles;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public static AssignRolesRequestBuilder builder() {
        return new AssignRolesRequestBuilder();
    }

    public static class AssignRolesRequestBuilder {
        private Set<String> roles;

        public AssignRolesRequestBuilder roles(Set<String> roles) {
            this.roles = roles;
            return this;
        }

        public AssignRolesRequest build() {
            return new AssignRolesRequest(roles);
        }
    }
}
