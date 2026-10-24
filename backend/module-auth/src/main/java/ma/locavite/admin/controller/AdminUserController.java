package ma.locavite.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.locavite.auth.dto.UserResponse;
import ma.locavite.user.dto.AssignRolesRequest;
import ma.locavite.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Users", description = "Endpoints d'administration des utilisateurs et attribution des rôles")
@SecurityRequirement(name = "BearerAuth")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lister les utilisateurs (ADMIN)", description = "Retourne la liste paginée de tous les utilisateurs inscrits sans exposer les mots de passe. Réservé aux administrateurs.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste paginée des utilisateurs récupérée"),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès interdit (rôle ADMIN requis)", content = @Content)
    })
    public ResponseEntity<Page<UserResponse>> getUsers(
            @Parameter(description = "Paramètres de pagination (page, size, sort)") Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Attribuer des rôles à un utilisateur (ADMIN)", description = "Modifie l'ensemble des rôles attribués à un utilisateur (CLIENT, AGENCE, ADMIN). Un administrateur ne peut pas se retirer lui-même le rôle ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rôles mis à jour avec succès",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Rôle invalide ou tentative d'auto-rétrogradation de l'administrateur", content = @Content),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès interdit (rôle ADMIN requis)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable", content = @Content)
    })
    public ResponseEntity<UserResponse> assignRoles(
            @Parameter(description = "Identifiant de l'utilisateur cible", required = true) @PathVariable Long id,
            @Valid @RequestBody AssignRolesRequest request) {
        return ResponseEntity.ok(userService.assignRole(id, request.getRoles()));
    }
}
