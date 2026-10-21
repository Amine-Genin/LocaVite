package ma.locavite.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.locavite.auth.dto.AuthResponse;
import ma.locavite.auth.dto.LoginRequest;
import ma.locavite.auth.dto.RefreshTokenRequest;
import ma.locavite.auth.dto.RegisterRequest;
import ma.locavite.auth.dto.UserResponse;
import ma.locavite.auth.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Endpoints d'authentification, inscription, rafraîchissement de token et déconnexion")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Inscription utilisateur", description = "Crée un nouveau compte utilisateur avec le rôle CLIENT par défaut et envoie un email de confirmation.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Utilisateur créé avec succès",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données d'inscription invalides ou email déjà utilisé",
                    content = @Content)
    })
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Connexion utilisateur", description = "Authentifie l'utilisateur via son email et mot de passe. Renvoie un JWT accessToken et un refreshToken.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentification réussie",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Format de la requête invalide",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Email ou mot de passe incorrect",
                    content = @Content)
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rafraîchir les tokens", description = "Génère un nouveau couple accessToken / refreshToken à partir d'un refreshToken valide.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens rafraîchis avec succès",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Refresh token invalide ou expiré",
                    content = @Content)
    })
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Déconnexion utilisateur", description = "Révoque le refreshToken fourni dans Redis.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Déconnexion réussie / Token révoqué",
                    content = @Content)
    })
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }
}
