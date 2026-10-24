package ma.locavite.document.service;

import io.minio.*;
import ma.locavite.document.config.MinioProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinioServiceTest {

    @Mock
    private MinioClient minioClient;

    private MinioProperties properties;
    private MinioService minioService;

    @BeforeEach
    void setUp() {
        properties = new MinioProperties();
        properties.setBucket("locavite-documents");
        properties.setEndpoint("http://localhost:9000");
        properties.setAccessKey("locavite");
        properties.setSecretKey("locavite_dev_password");
        properties.setInitBucket(true);

        minioService = new MinioService(minioClient, properties);
    }

    @Test
    @DisplayName("initBucket crée le bucket s'il n'existe pas")
    void testInitBucketCreatesWhenMissing() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        minioService.initBucket();

        verify(minioClient).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    @DisplayName("initBucket ne fait rien si le bucket existe déjà")
    void testInitBucketDoesNotCreateWhenAlreadyExists() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        minioService.initBucket();

        verify(minioClient).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    @DisplayName("initBucket est ignoré si initBucket=false")
    void testInitBucketSkippedWhenDisabled() throws Exception {
        properties.setInitBucket(false);

        minioService.initBucket();

        verify(minioClient, never()).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    @DisplayName("putObject appelle minioClient.putObject")
    void testPutObject() throws Exception {
        InputStream stream = new ByteArrayInputStream("test content".getBytes());

        minioService.putObject("test/path.pdf", stream, 12L, "application/pdf");

        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("getPresignedUrl génère une URL avec expiration par défaut (15 minutes)")
    void testGetPresignedUrl() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://localhost:9000/locavite-documents/test.pdf?signature=abc");

        String url = minioService.getPresignedUrl("test.pdf");

        assertThat(url).isEqualTo("http://localhost:9000/locavite-documents/test.pdf?signature=abc");
        verify(minioClient).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
    }

    @Test
    @DisplayName("deleteObject appelle minioClient.removeObject")
    void testDeleteObject() throws Exception {
        minioService.deleteObject("test/path.pdf");

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }
}
