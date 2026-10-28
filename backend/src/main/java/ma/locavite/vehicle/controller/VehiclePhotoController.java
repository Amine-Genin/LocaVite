package ma.locavite.vehicle.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.vehicle.dto.VehiclePhotoResponse;
import ma.locavite.vehicle.service.VehiclePhotoService;
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
@RequestMapping("/api/vehicles/{vehicleId}/photos")
@Tag(name = "Vehicle photos", description = "Gestion des photos des véhicules")
@SecurityRequirement(name = "BearerAuth")
public class VehiclePhotoController {

    private final VehiclePhotoService photoService;
    private final UserRepository userRepository;

    public VehiclePhotoController(VehiclePhotoService photoService, UserRepository userRepository) {
        this.photoService = photoService;
        this.userRepository = userRepository;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<VehiclePhotoResponse>> uploadPhotos(
            @PathVariable Long vehicleId,
            @RequestParam("files") List<MultipartFile> files,
            @Parameter(hidden = true) @AuthenticationPrincipal User currentUser,
            @Parameter(hidden = true) Authentication authentication) {
        List<VehiclePhotoResponse> photos = photoService.upload(vehicleId, resolveCurrentUser(currentUser, authentication), files);
        return ResponseEntity.status(HttpStatus.CREATED).body(photos);
    }

    @GetMapping
    public ResponseEntity<List<VehiclePhotoResponse>> getPhotos(
            @PathVariable Long vehicleId,
            @Parameter(hidden = true) @AuthenticationPrincipal User currentUser,
            @Parameter(hidden = true) Authentication authentication) {
        return ResponseEntity.ok(photoService.getPhotos(vehicleId, resolveCurrentUser(currentUser, authentication)));
    }

    @DeleteMapping("/{photoId}")
    public ResponseEntity<Void> deletePhoto(
            @PathVariable Long vehicleId,
            @PathVariable Long photoId,
            @Parameter(hidden = true) @AuthenticationPrincipal User currentUser,
            @Parameter(hidden = true) Authentication authentication) {
        photoService.delete(vehicleId, photoId, resolveCurrentUser(currentUser, authentication));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{photoId}/primary")
    public ResponseEntity<VehiclePhotoResponse> setPrimary(
            @PathVariable Long vehicleId,
            @PathVariable Long photoId,
            @Parameter(hidden = true) @AuthenticationPrincipal User currentUser,
            @Parameter(hidden = true) Authentication authentication) {
        return ResponseEntity.ok(photoService.setPrimary(
                vehicleId, photoId, resolveCurrentUser(currentUser, authentication)));
    }

    private User resolveCurrentUser(User currentUser, Authentication authentication) {
        if (currentUser != null) {
            return currentUser;
        }
        if (authentication != null && authentication.getName() != null) {
            return userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new AccessDeniedException("Utilisateur non trouvé"));
        }
        throw new AccessDeniedException("Authentification requise");
    }
}
