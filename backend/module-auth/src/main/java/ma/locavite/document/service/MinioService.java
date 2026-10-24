package ma.locavite.document.service;

import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import ma.locavite.document.config.MinioProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Service
public class MinioService {

    private static final Logger log = LoggerFactory.getLogger(MinioService.class);
    public static final int DEFAULT_EXPIRY_SECONDS = 15 * 60; // 15 minutes

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public MinioService(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @PostConstruct
    public void initBucket() {
        if (!properties.isInitBucket()) {
            log.info("L'initialisation du bucket MinIO est désactivée via configuration.");
            return;
        }

        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(properties.getBucket()).build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(properties.getBucket()).build()
                );
                log.info("Bucket MinIO '{}' créé avec succès au démarrage.", properties.getBucket());
            } else {
                log.info("Bucket MinIO '{}' est déjà existant.", properties.getBucket());
            }
        } catch (Exception e) {
            log.warn("Impossible d'initialiser le bucket MinIO '{}' : {}", properties.getBucket(), e.getMessage());
        }
    }

    public void putObject(String path, InputStream inputStream, long size, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(path)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );
            log.info("Objet uploadé avec succès dans MinIO : path={}", path);
        } catch (Exception e) {
            log.error("Erreur lors de l'upload vers MinIO [path={}] : {}", path, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'upload du fichier vers le stockage MinIO : " + e.getMessage(), e);
        }
    }

    public String getPresignedUrl(String path) {
        return getPresignedUrl(path, DEFAULT_EXPIRY_SECONDS);
    }

    public String getPresignedUrl(String path, int expiryInSeconds) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(properties.getBucket())
                            .object(path)
                            .expiry(expiryInSeconds, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Erreur lors de la génération de l'URL pré-signée [path={}] : {}", path, e.getMessage(), e);
            return null;
        }
    }

    public void deleteObject(String path) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(path)
                            .build()
            );
            log.info("Objet supprimé de MinIO : path={}", path);
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de l'objet MinIO [path={}] : {}", path, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la suppression du fichier dans MinIO : " + e.getMessage(), e);
        }
    }

    public String getBucket() {
        return properties.getBucket();
    }
}
