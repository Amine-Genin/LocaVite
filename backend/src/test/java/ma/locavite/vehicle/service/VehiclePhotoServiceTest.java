package ma.locavite.vehicle.service;

import ma.locavite.auth.domain.User;
import ma.locavite.document.exception.FileTooLargeException;
import ma.locavite.vehicle.domain.VehiclePhoto;
import ma.locavite.vehicle.dto.VehiclePhotoResponse;
import ma.locavite.vehicle.exception.InvalidVehiclePhotoException;
import ma.locavite.vehicle.exception.VehicleNotFoundException;
import ma.locavite.vehicle.exception.VehiclePhotoNotFoundException;
import ma.locavite.vehicle.repository.VehiclePhotoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehiclePhotoServiceTest {

    private static final Long VEHICLE_ID = 42L;
    private static final Long AGENCY_ID = 7L;

    @Mock
    private VehiclePhotoRepository photoRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private VehiclePhotoStorageService storageService;

    private VehiclePhotoService photoService;
    private User owner;
    private byte[] png;

    @BeforeEach
    void setUp() throws IOException {
        photoService = new VehiclePhotoService(photoRepository, jdbcTemplate, storageService,
                new ImageCompressionService());
        owner = User.builder().id(AGENCY_ID).email("agency@example.ma").build();
        BufferedImage image = new BufferedImage(600, 400, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        png = output.toByteArray();
    }

    @Test
    void uploadThreePhotosCreatesThreeEntitiesAndMakesTheFirstPrimary() throws IOException {
        whenVehicleOwned();
        when(photoRepository.findByVehicleIdOrderByOrdreAsc(VEHICLE_ID)).thenReturn(List.of());
        when(storageService.getPresignedUrl(anyString())).thenAnswer(invocation ->
                "https://minio.example/" + invocation.getArgument(0));
        when(photoRepository.save(any(VehiclePhoto.class))).thenAnswer(invocation -> {
            VehiclePhoto photo = invocation.getArgument(0);
            photo.setId(100L);
            photo.setUploadedAt(LocalDateTime.now());
            return photo;
        });

        List<VehiclePhotoResponse> uploaded = photoService.upload(
                VEHICLE_ID, owner, List.of(photo(), photo(), photo()));

        assertThat(uploaded).hasSize(3);
        assertThat(uploaded).extracting(VehiclePhotoResponse::isPrimary).containsExactly(true, false, false);
        ArgumentCaptor<VehiclePhoto> photoCaptor = ArgumentCaptor.forClass(VehiclePhoto.class);
        verify(photoRepository, times(3)).save(photoCaptor.capture());
        assertThat(photoCaptor.getAllValues()).extracting(VehiclePhoto::isPrimary)
                .containsExactly(true, false, false);
        verify(storageService, times(3)).store(argThat(path -> path.matches("^42/[a-f0-9-]+\\.png$")),
                any(), eq("image/png"));
        verify(storageService, times(3)).store(argThat(path -> path.matches("^42/[a-f0-9-]+_thumbnail\\.jpg$")),
                any(), eq("image/jpeg"));
        BufferedImage thumbnail = ImageIO.read(new java.io.ByteArrayInputStream(compressionServiceThumbnail()));
        assertThat(thumbnail.getWidth()).isEqualTo(300);
        assertThat(thumbnail.getHeight()).isEqualTo(300);
    }

    @Test
    void rejectsElevenPhotos() {
        whenVehicleOwned();

        assertThatThrownBy(() -> photoService.upload(VEHICLE_ID, owner,
                java.util.Collections.nCopies(11, photo())))
                .isInstanceOf(InvalidVehiclePhotoException.class)
                .hasMessageContaining("10");

        verifyNoInteractions(storageService);
        verify(photoRepository, never()).save(any());
    }

    @Test
    void rejectsUploadWhenExistingPhotosWouldExceedLimit() {
        whenVehicleOwned();
        when(photoRepository.findByVehicleIdOrderByOrdreAsc(VEHICLE_ID))
                .thenReturn(java.util.Collections.nCopies(10, vehiclePhoto(1L, false, 1)));

        assertThatThrownBy(() -> photoService.upload(VEHICLE_ID, owner, List.of(photo())))
                .isInstanceOf(InvalidVehiclePhotoException.class)
                .hasMessageContaining("10 photos");

        verifyNoInteractions(storageService);
        verify(photoRepository, never()).save(any());
    }

    @Test
    void rejectsMissingOrEmptyUploads() {
        whenVehicleOwned();

        assertThatThrownBy(() -> photoService.upload(VEHICLE_ID, owner, null))
                .isInstanceOf(InvalidVehiclePhotoException.class)
                .hasMessageContaining("Au moins une photo");
        assertThatThrownBy(() -> photoService.upload(VEHICLE_ID, owner, List.of()))
                .isInstanceOf(InvalidVehiclePhotoException.class)
                .hasMessageContaining("Au moins une photo");
        assertThatThrownBy(() -> photoService.upload(VEHICLE_ID, owner,
                List.of(new MockMultipartFile("files", "empty.png", "image/png", new byte[0]))))
                .isInstanceOf(InvalidVehiclePhotoException.class)
                .hasMessageContaining("vides");

        verifyNoInteractions(storageService);
    }

    @Test
    void rejectsUnauthenticatedAndMissingVehicleRequests() {
        assertThatThrownBy(() -> photoService.getPhotos(VEHICLE_ID, null))
                .isInstanceOf(AccessDeniedException.class);

        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(VEHICLE_ID)))
                .thenReturn(List.of());
        assertThatThrownBy(() -> photoService.getPhotos(VEHICLE_ID, owner))
                .isInstanceOf(VehicleNotFoundException.class);
        assertThatThrownBy(() -> photoService.getPhotos(null, owner))
                .isInstanceOf(VehicleNotFoundException.class);

        verifyNoInteractions(photoRepository, storageService);
    }

    @Test
    void rejectsFilesThatAreNotSupportedImages() {
        whenVehicleOwned();
        when(photoRepository.findByVehicleIdOrderByOrdreAsc(VEHICLE_ID)).thenReturn(List.of());
        MockMultipartFile textFile = new MockMultipartFile("files", "notes.txt", "text/plain",
                "not an image".getBytes());

        assertThatThrownBy(() -> photoService.upload(VEHICLE_ID, owner, List.of(textFile)))
                .isInstanceOf(InvalidVehiclePhotoException.class)
                .hasMessageContaining("Types acceptés");

        verifyNoInteractions(storageService);
    }

    @Test
    void getsPhotosAsSignedResponses() {
        whenVehicleOwned();
        VehiclePhoto photo = vehiclePhoto(1L, true, 1);
        photo.setUploadedAt(LocalDateTime.now());
        when(photoRepository.findByVehicleIdOrderByOrdreAsc(VEHICLE_ID)).thenReturn(List.of(photo));
        when(storageService.getPresignedUrl(anyString())).thenAnswer(invocation ->
                "https://minio.example/" + invocation.getArgument(0));

        List<VehiclePhotoResponse> responses = photoService.getPhotos(VEHICLE_ID, owner);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).url()).endsWith("42/1.png");
        assertThat(responses.get(0).thumbnailUrl()).endsWith("42/1_thumbnail.jpg");
        assertThat(responses.get(0).isPrimary()).isTrue();
    }

    @Test
    void selectingPrimaryClearsOtherPhotos() {
        whenVehicleOwned();
        VehiclePhoto first = vehiclePhoto(1L, true, 1);
        VehiclePhoto selected = vehiclePhoto(2L, false, 2);
        when(photoRepository.findByIdAndVehicleId(2L, VEHICLE_ID)).thenReturn(Optional.of(selected));
        when(photoRepository.findByVehicleIdOrderByOrdreAsc(VEHICLE_ID))
                .thenReturn(new ArrayList<>(List.of(first, selected)));
        when(storageService.getPresignedUrl(anyString())).thenAnswer(invocation ->
                "https://minio.example/" + invocation.getArgument(0));

        VehiclePhotoResponse response = photoService.setPrimary(VEHICLE_ID, 2L, owner);

        assertThat(first.isPrimary()).isFalse();
        assertThat(selected.isPrimary()).isTrue();
        assertThat(response.id()).isEqualTo(2L);
        verify(photoRepository).saveAllAndFlush(anyList());
        verify(photoRepository).save(selected);
    }

    @Test
    void deletingNonPrimaryDoesNotChangeThePrimaryPhoto() {
        whenVehicleOwned();
        VehiclePhoto selected = vehiclePhoto(2L, false, 2);
        VehiclePhoto primary = vehiclePhoto(1L, true, 1);
        when(photoRepository.findByIdAndVehicleId(2L, VEHICLE_ID)).thenReturn(Optional.of(selected));
        when(photoRepository.findByVehicleIdOrderByOrdreAsc(VEHICLE_ID))
                .thenReturn(List.of(primary, selected));

        photoService.delete(VEHICLE_ID, 2L, owner);

        assertThat(primary.isPrimary()).isTrue();
        verify(photoRepository, never()).save(primary);
        verify(storageService).delete(selected.getPath());
        verify(storageService).delete(selected.getThumbnailPath());
    }

    @Test
    void rejectsDeletionOrSelectionOfUnknownPhoto() {
        whenVehicleOwned();
        when(photoRepository.findByIdAndVehicleId(99L, VEHICLE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> photoService.delete(VEHICLE_ID, 99L, owner))
                .isInstanceOf(VehiclePhotoNotFoundException.class);
        assertThatThrownBy(() -> photoService.setPrimary(VEHICLE_ID, 99L, owner))
                .isInstanceOf(VehiclePhotoNotFoundException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void cleansUpStoredOriginalWhenThumbnailUploadFails() {
        whenVehicleOwned();
        when(photoRepository.findByVehicleIdOrderByOrdreAsc(VEHICLE_ID)).thenReturn(List.of());
        AtomicInteger storeCalls = new AtomicInteger();
        doAnswer(invocation -> {
            if (storeCalls.incrementAndGet() == 2) {
                throw new IllegalStateException("thumbnail storage failed");
            }
            return null;
        }).when(storageService).store(anyString(), any(), anyString());

        assertThatThrownBy(() -> photoService.upload(VEHICLE_ID, owner, List.of(photo())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("thumbnail storage failed");

        verify(storageService).delete(argThat(path -> path.matches("^42/[a-f0-9-]+\\.png$")));
        verify(photoRepository, never()).save(any());
    }

    @Test
    void deletingPrimaryPromotesNextPhoto() {
        whenVehicleOwned();
        VehiclePhoto primary = vehiclePhoto(1L, true, 1);
        VehiclePhoto next = vehiclePhoto(2L, false, 2);
        when(photoRepository.findByIdAndVehicleId(1L, VEHICLE_ID)).thenReturn(Optional.of(primary));
        when(photoRepository.findByVehicleIdOrderByOrdreAsc(VEHICLE_ID)).thenReturn(List.of(primary, next));

        photoService.delete(VEHICLE_ID, 1L, owner);

        verify(photoRepository).delete(primary);
        assertThat(next.isPrimary()).isTrue();
        verify(photoRepository).save(next);
        verify(storageService).delete(primary.getPath());
        verify(storageService).delete(primary.getThumbnailPath());
    }

    @Test
    void deniesPhotoAccessToNonOwner() {
        User otherUser = User.builder().id(99L).email("other@example.ma").build();
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(VEHICLE_ID)))
                .thenReturn(List.of(AGENCY_ID));

        assertThatThrownBy(() -> photoService.getPhotos(VEHICLE_ID, otherUser))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(photoRepository);
    }

    @Test
    void rejectsPhotosLargerThanEightMegabytes() {
        whenVehicleOwned();
        MockMultipartFile largeFile = new MockMultipartFile("files", "large.png", "image/png",
                new byte[(int) VehiclePhotoService.MAX_PHOTO_SIZE + 1]);
        when(photoRepository.findByVehicleIdOrderByOrdreAsc(VEHICLE_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> photoService.upload(VEHICLE_ID, owner, List.of(largeFile)))
                .isInstanceOf(FileTooLargeException.class)
                .hasMessageContaining("8 Mo");
        verifyNoInteractions(storageService);
    }

    private MockMultipartFile photo() {
        return new MockMultipartFile("files", "vehicle.png", "image/png", png);
    }

    private void whenVehicleOwned() {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(VEHICLE_ID)))
                .thenReturn(List.of(AGENCY_ID));
    }

    private byte[] compressionServiceThumbnail() {
        return new ImageCompressionService().createThumbnail(png);
    }

    private VehiclePhoto vehiclePhoto(Long id, boolean primary, int ordre) {
        VehiclePhoto photo = new VehiclePhoto();
        photo.setId(id);
        photo.setVehicleId(VEHICLE_ID);
        photo.setPath("42/" + id + ".png");
        photo.setThumbnailPath("42/" + id + "_thumbnail.jpg");
        photo.setPrimary(primary);
        photo.setOrdre(ordre);
        return photo;
    }
}
