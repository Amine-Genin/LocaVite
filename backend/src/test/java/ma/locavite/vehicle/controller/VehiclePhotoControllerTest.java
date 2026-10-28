package ma.locavite.vehicle.controller;

import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.vehicle.dto.VehiclePhotoResponse;
import ma.locavite.vehicle.service.VehiclePhotoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehiclePhotoControllerTest {

    private static final Long VEHICLE_ID = 42L;
    private static final Long PHOTO_ID = 5L;

    @Mock
    private VehiclePhotoService photoService;

    @Mock
    private UserRepository userRepository;

    private VehiclePhotoController controller;
    private User user;
    private VehiclePhotoResponse photoResponse;

    @BeforeEach
    void setUp() {
        controller = new VehiclePhotoController(photoService, userRepository);
        user = User.builder().id(7L).email("agency@example.ma").build();
        photoResponse = new VehiclePhotoResponse(PHOTO_ID, VEHICLE_ID, "photo-url",
                "thumbnail-url", true, 1, LocalDateTime.now());
    }

    @Test
    void uploadReturnsCreatedAndUsesAuthenticatedUser() {
        MultipartFile file = new MockMultipartFile("files", "car.png", "image/png", new byte[]{1});
        when(photoService.upload(VEHICLE_ID, user, List.of(file))).thenReturn(List.of(photoResponse));

        var response = controller.uploadPhotos(VEHICLE_ID, List.of(file), user, authentication());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsExactly(photoResponse);
        verify(photoService).upload(VEHICLE_ID, user, List.of(file));
        verifyNoInteractions(userRepository);
    }

    @Test
    void getPhotosResolvesUserFromAuthenticationWhenPrincipalIsUnavailable() {
        when(userRepository.findByEmail("agency@example.ma")).thenReturn(Optional.of(user));
        when(photoService.getPhotos(VEHICLE_ID, user)).thenReturn(List.of(photoResponse));

        var response = controller.getPhotos(VEHICLE_ID, null, authentication());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(photoResponse);
        verify(photoService).getPhotos(VEHICLE_ID, user);
    }

    @Test
    void deleteReturnsNoContent() {
        var response = controller.deletePhoto(VEHICLE_ID, PHOTO_ID, user, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(photoService).delete(VEHICLE_ID, PHOTO_ID, user);
    }

    @Test
    void setPrimaryReturnsUpdatedPhoto() {
        when(photoService.setPrimary(VEHICLE_ID, PHOTO_ID, user)).thenReturn(photoResponse);

        var response = controller.setPrimary(VEHICLE_ID, PHOTO_ID, user, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(photoResponse);
        verify(photoService).setPrimary(VEHICLE_ID, PHOTO_ID, user);
    }

    @Test
    void rejectsAuthenticationWithoutPrincipalOrAuthenticationName() {
        assertThatThrownBy(() -> controller.getPhotos(VEHICLE_ID, null, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Authentification requise");
        verifyNoInteractions(photoService, userRepository);
    }

    @Test
    void rejectsAuthenticationWhenEmailIsNotFound() {
        when(userRepository.findByEmail("agency@example.ma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getPhotos(VEHICLE_ID, null, authentication()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Utilisateur non trouvé");
        verifyNoInteractions(photoService);
    }

    private Authentication authentication() {
        return new TestingAuthenticationToken("agency@example.ma", "password", "ROLE_AGENCE");
    }
}
