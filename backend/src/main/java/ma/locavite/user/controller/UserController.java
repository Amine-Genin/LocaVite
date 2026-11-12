package ma.locavite.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.locavite.auth.domain.User;
import ma.locavite.auth.dto.UserResponse;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.user.dto.UpdateProfileRequest;
import ma.locavite.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
@Tag(name = "Users", description = "Endpoints de gestion du profil de l'utilisateur connecté")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(summary = "Consulter son profil", description = "Retourne les informations du profil de l'utilisateur actuellement authentifié via JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil récupéré avec succès",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Non authentifié (token JWT manquant ou invalide)",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès refusé",
                    content = @Content)
    })
    public ResponseEntity<UserResponse> getMe(
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @AuthenticationPrincipal User current,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) Authentication authentication) {
        User user = resolveCurrentUser(current, authentication);
        return ResponseEntity.ok(userService.getProfile(user.getId()));
    }

    @PutMapping
    @Operation(summary = "Mettre à jour son profil", description = "Met à jour les informations personnelles (nom, prénom, téléphone, préférences notifications) de l'utilisateur connecté.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil mis à jour avec succès",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données de formulaire invalides",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Non authentifié",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès refusé",
                    content = @Content)
    })
    public ResponseEntity<UserResponse> updateMe(
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @AuthenticationPrincipal User current,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        User user = resolveCurrentUser(current, authentication);
        return ResponseEntity.ok(userService.updateProfile(user.getId(), request));
    }

    @PatchMapping("/notifications/opt-out")
    @Operation(summary = "Désactiver les notifications SMS/WhatsApp", description = "Permet au client de s'opposer (opt-out) aux notifications SMS/WhatsApp.")
    public ResponseEntity<UserResponse> optOut(
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @AuthenticationPrincipal User current,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) Authentication authentication) {
        User user = resolveCurrentUser(current, authentication);
        return ResponseEntity.ok(userService.optOutNotifications(user.getId()));
    }

    @PatchMapping("/notifications/opt-in")
    @Operation(summary = "Activer les notifications SMS/WhatsApp", description = "Permet au client d'activer (opt-in) les notifications SMS/WhatsApp avec son canal préféré.")
    public ResponseEntity<UserResponse> optIn(
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @AuthenticationPrincipal User current,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) Authentication authentication,
            @RequestParam(required = false, defaultValue = "SMS") String channel) {
        User user = resolveCurrentUser(current, authentication);
        ma.locavite.notification.domain.NotificationPreference pref =
                ma.locavite.notification.domain.NotificationPreference.from(channel);
        return ResponseEntity.ok(userService.optInNotifications(user.getId(), pref));
    }

    private User resolveCurrentUser(User currentUser, Authentication authentication) {
        if (currentUser != null) {
            return currentUser;
        }
        if (authentication != null) {
            if (authentication.getPrincipal() instanceof User user) {
                return user;
            }
            if (authentication.getName() != null) {
                return userRepository.findByEmail(authentication.getName())
                        .orElseThrow(() -> new AccessDeniedException("Utilisateur non trouvé"));
            }
        }
        throw new AccessDeniedException("Authentification requise");
    }
}
