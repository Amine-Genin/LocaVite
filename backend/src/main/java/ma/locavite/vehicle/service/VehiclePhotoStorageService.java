package ma.locavite.vehicle.service;

import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import ma.locavite.document.config.MinioProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;

@Service
public class VehiclePhotoStorageService {

    public static final int PRESIGNED_URL_EXPIRY_SECONDS = 60 * 60;

    private static final Logger log = LoggerFactory.getLogger(VehiclePhotoStorageService.class);
    private final MinioClient minioClient;
    private final MinioProperties properties;
    private final String bucket;

    public VehiclePhotoStorageService(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
        this.bucket = properties.getVehiclesBucket();
    }

    @PostConstruct
    public void initializeBucket() {
        if (!properties.isInitBucket()) {
            log.info("L'initialisation du bucket MinIO des véhicules est désactivée via configuration.");
            return;
        }

        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Bucket MinIO '{}' créé.", bucket);
            }
        } catch (Exception e) {
            log.error("Impossible d'initialiser le bucket MinIO '{}'", bucket, e);
            throw new IllegalStateException("Impossible d'initialiser le stockage des photos de véhicules", e);
        }
    }

    public void store(String path, byte[] contents, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(path)
                    .stream(new ByteArrayInputStream(contents), contents.length, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("Impossible d'enregistrer la photo dans MinIO", e);
        }
    }

    public String getPresignedUrl(String path) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(path)
                    .expiry(PRESIGNED_URL_EXPIRY_SECONDS, TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de générer l'URL de consultation de la photo", e);
        }
    }

    public void delete(String path) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(path)
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de supprimer la photo de MinIO", e);
        }
    }
}
