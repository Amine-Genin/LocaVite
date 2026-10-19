package ma.locavite.user.controller;

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
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<UserResponse> getMe(
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @AuthenticationPrincipal User current,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) Authentication authentication) {
        User user = resolveCurrentUser(current, authentication);
        return ResponseEntity.ok(userService.getProfile(user.getId()));
    }

    @PutMapping
    public ResponseEntity<UserResponse> updateMe(
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) @AuthenticationPrincipal User current,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true) Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        User user = resolveCurrentUser(current, authentication);
        return ResponseEntity.ok(userService.updateProfile(user.getId(), request));
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