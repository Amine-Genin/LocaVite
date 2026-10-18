package ma.locavite.admin.controller;

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
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> assignRoles(
            @PathVariable Long id,
            @Valid @RequestBody AssignRolesRequest request) {
        return ResponseEntity.ok(userService.assignRole(id, request.getRoles()));
    }
}
