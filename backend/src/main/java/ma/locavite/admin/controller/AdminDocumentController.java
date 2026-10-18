package ma.locavite.admin.controller;

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
public class AdminDocumentController {

    private final DocumentService documentService;
    private final UserRepository userRepository;

    public AdminDocumentController(DocumentService documentService, UserRepository userRepository) {
        this.documentService = documentService;
        this.userRepository = userRepository;
    }

    @PutMapping("/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentResponse> verifyDocument(
            @PathVariable Long id,
            @Valid @RequestBody VerifyDocumentRequest request,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @AuthenticationPrincipal User adminUser,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) Authentication authentication) {
        User user = resolveCurrentUser(adminUser, authentication);
        DocumentResponse response = documentService.verifyDocument(id, request, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentResponse> getDocument(
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @AuthenticationPrincipal User adminUser,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) Authentication authentication) {
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
