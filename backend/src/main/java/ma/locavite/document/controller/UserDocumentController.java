package ma.locavite.document.controller;

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
public class UserDocumentController {

    private final DocumentService documentService;
    private final UserRepository userRepository;

    public UserDocumentController(DocumentService documentService, UserRepository userRepository) {
        this.documentService = documentService;
        this.userRepository = userRepository;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("type") DocumentType type,
            @RequestParam("file") MultipartFile file,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @AuthenticationPrincipal User currentUser,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) Authentication authentication) {
        User user = resolveCurrentUser(currentUser, authentication);
        DocumentResponse response = documentService.upload(user, type, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getUserDocuments(
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @AuthenticationPrincipal User currentUser,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) Authentication authentication) {
        User user = resolveCurrentUser(currentUser, authentication);
        return ResponseEntity.ok(documentService.getUserDocuments(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @AuthenticationPrincipal User currentUser,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) Authentication authentication) {
        User user = resolveCurrentUser(currentUser, authentication);
        return ResponseEntity.ok(documentService.getDocumentById(user, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @AuthenticationPrincipal User currentUser,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) Authentication authentication) {
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
