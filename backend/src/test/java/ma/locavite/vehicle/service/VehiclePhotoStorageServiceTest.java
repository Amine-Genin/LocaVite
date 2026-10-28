package ma.locavite.vehicle.service;

import io.minio.*;
import ma.locavite.document.config.MinioProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehiclePhotoStorageServiceTest {

    @Mock
    private MinioClient minioClient;

    private MinioProperties properties;
    private VehiclePhotoStorageService storageService;

    @BeforeEach
    void setUp() {
        properties = new MinioProperties();
        properties.setVehiclesBucket("locavite-vehicles");
        storageService = new VehiclePhotoStorageService(minioClient, properties);
    }

    @Test
    void initializeBucketCreatesBucketWhenMissing() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        storageService.initializeBucket();

        verify(minioClient).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void initializeBucketLeavesExistingBucketUnchanged() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        storageService.initializeBucket();

        verify(minioClient).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void initializeBucketDoesNothingWhenDisabled() {
        properties.setInitBucket(false);

        storageService.initializeBucket();

        verifyNoInteractions(minioClient);
    }

    @Test
    void initializeBucketReportsMinioFailures() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
                .thenThrow(new RuntimeException("MinIO unavailable"));

        assertThatThrownBy(() -> storageService.initializeBucket())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("initialiser")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void storeUploadsOriginalOrThumbnail() throws Exception {
        storageService.store("42/photo.png", new byte[]{1, 2, 3}, "image/png");

        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void storeReportsMinioFailures() throws Exception {
        doThrow(new RuntimeException("MinIO unavailable"))
                .when(minioClient).putObject(any(PutObjectArgs.class));

        assertThatThrownBy(() -> storageService.store("42/photo.png", new byte[]{1}, "image/png"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("enregistrer")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void returnsPresignedUrlWithOneHourExpiry() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("https://minio.example/42/photo.png?signature=valid");

        assertThat(storageService.getPresignedUrl("42/photo.png"))
                .isEqualTo("https://minio.example/42/photo.png?signature=valid");
        assertThat(VehiclePhotoStorageService.PRESIGNED_URL_EXPIRY_SECONDS).isEqualTo(3600);
        verify(minioClient).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
    }

    @Test
    void presignedUrlReportsMinioFailures() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenThrow(new RuntimeException("MinIO unavailable"));

        assertThatThrownBy(() -> storageService.getPresignedUrl("42/photo.png"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("générer")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void deleteRemovesObject() throws Exception {
        storageService.delete("42/photo.png");

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    void deleteReportsMinioFailures() throws Exception {
        doThrow(new RuntimeException("MinIO unavailable"))
                .when(minioClient).removeObject(any(RemoveObjectArgs.class));

        assertThatThrownBy(() -> storageService.delete("42/photo.png"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("supprimer")
                .hasCauseInstanceOf(RuntimeException.class);
    }
}
