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
import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.document.dto.DocumentResponse;
import ma.locavite.document.dto.VerifyDocumentRequest;
import ma.locavite.document.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/documents")
@Tag(name = "Documents", description = "Endpoints d'administration et de validation des documents utilisateurs")
@SecurityRequirement(name = "BearerAuth")
public class AdminDocumentController {

    private final DocumentService documentService;
    private final UserRepository userRepository;

    public AdminDocumentController(DocumentService documentService, UserRepository userRepository) {
        this.documentService = documentService;
        this.userRepository = userRepository;
    }

    @PutMapping("/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Valider ou rejeter un document (ADMIN)", description = "Met à jour le statut de validation du document (VALIDATED ou REJECTED). Réservé aux administrateurs.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Document vérifié avec succès",
                    content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide (statut non supporté)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès interdit (rôle ADMIN requis)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Document introuvable", content = @Content)
    })
    public ResponseEntity<DocumentResponse> verifyDocument(
            @Parameter(description = "Identifiant du document à vérifier", required = true) @PathVariable Long id,
            @Valid @RequestBody VerifyDocumentRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal User adminUser,
            @Parameter(hidden = true) Authentication authentication) {
        User user = resolveCurrentUser(adminUser, authentication);
        DocumentResponse response = documentService.verifyDocument(id, request, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Consulter un document utilisateur (ADMIN)", description = "Accède aux détails et à l'URL de téléchargement d'un document utilisateur. Réservé aux administrateurs.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Document récupéré avec succès",
                    content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès interdit (rôle ADMIN requis)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Document introuvable", content = @Content)
    })
    public ResponseEntity<DocumentResponse> getDocument(
            @Parameter(description = "Identifiant du document", required = true) @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal User adminUser,
            @Parameter(hidden = true) Authentication authentication) {
        User user = resolveCurrentUser(adminUser, authentication);
        DocumentResponse response = documentService.getDocumentById(user, id);
        return ResponseEntity.ok(response);
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
