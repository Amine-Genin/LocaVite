package ma.locavite.document.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.document.domain.DocumentType;
import ma.locavite.document.dto.DocumentResponse;
import ma.locavite.document.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/users/me/documents")
@Tag(name = "Documents", description = "Endpoints de gestion des documents justificatifs de l'utilisateur (CIN, Permis)")
@SecurityRequirement(name = "BearerAuth")
public class UserDocumentController {

    private final DocumentService documentService;
    private final UserRepository userRepository;

    public UserDocumentController(DocumentService documentService, UserRepository userRepository) {
        this.documentService = documentService;
        this.userRepository = userRepository;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Téléverser un document", description = "Téléverse un document (CIN ou PERMIS) sous format PDF, PNG ou JPEG vers le stockage MinIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Document téléversé avec succès",
                    content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Fichier invalide (type non supporté ou taille max dépassée)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content)
    })
    public ResponseEntity<DocumentResponse> uploadDocument(
            @Parameter(description = "Type de document (CIN, PERMIS)", required = true) @RequestParam("type") DocumentType type,
            @Parameter(description = "Fichier du document (PDF, PNG, JPG - max 5 Mo)", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(hidden = true) @AuthenticationPrincipal User currentUser,
            @Parameter(hidden = true) Authentication authentication) {
        User user = resolveCurrentUser(currentUser, authentication);
        DocumentResponse response = documentService.upload(user, type, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Lister ses documents", description = "Retourne tous les documents téléversés par l'utilisateur connecté avec leurs URL de visualisation sécurisées.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des documents récupérée"),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content)
    })
    public ResponseEntity<List<DocumentResponse>> getUserDocuments(
            @Parameter(hidden = true) @AuthenticationPrincipal User currentUser,
            @Parameter(hidden = true) Authentication authentication) {
        User user = resolveCurrentUser(currentUser, authentication);
        return ResponseEntity.ok(documentService.getUserDocuments(user));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un document", description = "Retourne le document spécifié par son ID s'il appartient à l'utilisateur connecté.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Document récupéré avec succès",
                    content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès non autorisé à ce document", content = @Content),
            @ApiResponse(responseCode = "404", description = "Document introuvable", content = @Content)
    })
    public ResponseEntity<DocumentResponse> getDocument(
            @Parameter(description = "Identifiant du document", required = true) @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal User currentUser,
            @Parameter(hidden = true) Authentication authentication) {
        User user = resolveCurrentUser(currentUser, authentication);
        return ResponseEntity.ok(documentService.getDocumentById(user, id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un document", description = "Supprime le document de la base et du stockage MinIO s'il appartient à l'utilisateur connecté.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Document supprimé avec succès"),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès non autorisé", content = @Content),
            @ApiResponse(responseCode = "404", description = "Document introuvable", content = @Content)
    })
    public ResponseEntity<Void> deleteDocument(
            @Parameter(description = "Identifiant du document", required = true) @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal User currentUser,
            @Parameter(hidden = true) Authentication authentication) {
        User user = resolveCurrentUser(currentUser, authentication);
        documentService.deleteDocument(user, id);
        return ResponseEntity.noContent().build();
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
