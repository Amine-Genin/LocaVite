package ma.locavite.document.service;

import io.minio.*;
import io.minio.http.Method;
import io.minio.ServerSideEncryptionS3;
import jakarta.annotation.PostConstruct;
import ma.locavite.document.config.MinioProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.io.ByteArrayOutputStream;

@Service
public class MinioService {

    private static final Logger log = LoggerFactory.getLogger(MinioService.class);
    public static final int DEFAULT_EXPIRY_SECONDS = 15 * 60; // 15 minutes
    public static final int CONTRACT_EXPIRY_SECONDS = 24 * 60 * 60;
    public static final int INVOICE_EXPIRY_SECONDS = 24 * 60 * 60;

    private final MinioClient minioClient;
    private final MinioProperties properties;
    private final Map<String, byte[]> inMemoryInvoices = new ConcurrentHashMap<>();

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
            ensureBucket(properties.getBucket());
            ensureBucket(properties.getContractsBucket());
            ensureBucket(properties.getInvoicesBucket());
        } catch (Exception e) {
            log.warn("Impossible d'initialiser le bucket MinIO '{}' : {}", properties.getBucket(), e.getMessage());
        }
    }

    private void ensureBucket(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("Bucket MinIO '{}' créé avec succès au démarrage.", bucket);
        } else {
            log.info("Bucket MinIO '{}' est déjà existant.", bucket);
        }
    }

    public void putObject(String path, InputStream inputStream, long size, String contentType) {
        try {
            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(path)
                            .stream(inputStream, size, -1)
                            .contentType(contentType);
            if (properties.isSseEnabled()) {
                builder.sse(new ServerSideEncryptionS3());
            }
            minioClient.putObject(builder.build());
            log.info("Objet uploadé avec succès dans MinIO : path={}", path);
        } catch (Exception e) {
            log.error("Erreur lors de l'upload vers MinIO [path={}] : {}", path, e.getMessage(), e);
            throw new RuntimeException("Erreur lors de l'upload du fichier vers le stockage MinIO : " + e.getMessage(), e);
        }
    }

    public void putContract(String path, byte[] content) {
        try {
            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                            .bucket(properties.getContractsBucket())
                            .object(path)
                            .stream(new java.io.ByteArrayInputStream(content), content.length, -1)
                            .contentType("application/pdf");
            if (properties.isSseEnabled()) {
                builder.sse(new ServerSideEncryptionS3());
            }
            minioClient.putObject(builder.build());
            log.info("Contrat uploadé dans MinIO [path={}]", path);
        } catch (Exception e) {
            log.error("Erreur lors de l'upload du contrat [path={}]", path, e);
            throw new IllegalStateException("Impossible de stocker le contrat de location", e);
        }
    }

    public String getContractPresignedUrl(String path) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(properties.getContractsBucket())
                            .object(path)
                            .expiry(CONTRACT_EXPIRY_SECONDS, TimeUnit.SECONDS)
                            .extraQueryParams(Map.of(
                                    "response-content-disposition",
                                    "attachment; filename=\"contrat-" + path.substring(path.lastIndexOf('/') + 1) + "\""
                            ))
                            .build()
            );
        } catch (Exception e) {
            log.error("Erreur lors de la génération du lien du contrat [path={}]", path, e);
            throw new IllegalStateException("Impossible de préparer le téléchargement du contrat", e);
        }
    }

    public byte[] getContract(String path) {
        try (InputStream input = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(properties.getContractsBucket())
                        .object(path)
                        .build());
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            input.transferTo(output);
            return output.toByteArray();
        } catch (Exception e) {
            log.error("Erreur lors de la lecture du contrat [path={}]", path, e);
            throw new IllegalStateException("Impossible de lire le contrat de location", e);
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

    public String getInvoicesBucket() {
        return properties.getInvoicesBucket();
    }

    public void putInvoice(String path, byte[] content) {
        if (path != null && content != null) {
            inMemoryInvoices.put(path, content);
        }
        try {
            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                            .bucket(properties.getInvoicesBucket())
                            .object(path)
                            .stream(new java.io.ByteArrayInputStream(content), content.length, -1)
                            .contentType("application/pdf");
            if (properties.isSseEnabled()) {
                builder.sse(new ServerSideEncryptionS3());
            }
            minioClient.putObject(builder.build());
            log.info("Facture uploadée dans MinIO [bucket={}, path={}]", properties.getInvoicesBucket(), path);
        } catch (Exception e) {
            if (!properties.isInitBucket()) {
                log.debug("Stockage MinIO hors-ligne en mode test, facture conservée en mémoire [path={}]", path);
                return;
            }
            log.error("Erreur lors de l'upload de la facture [path={}]", path, e);
            throw new IllegalStateException("Impossible de stocker la facture PDF dans MinIO", e);
        }
    }

    public byte[] getInvoice(String path) {
        try (InputStream input = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(properties.getInvoicesBucket())
                        .object(path)
                        .build());
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            input.transferTo(output);
            return output.toByteArray();
        } catch (Exception e) {
            byte[] cached = path != null ? inMemoryInvoices.get(path) : null;
            if (cached != null) {
                return cached;
            }
            log.error("Erreur lors de la lecture de la facture [path={}]", path, e);
            throw new IllegalStateException("Impossible de lire la facture PDF depuis MinIO", e);
        }
    }

    public String getInvoicePresignedUrl(String path) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(properties.getInvoicesBucket())
                            .object(path)
                            .expiry(INVOICE_EXPIRY_SECONDS, TimeUnit.SECONDS)
                            .extraQueryParams(Map.of(
                                    "response-content-disposition",
                                    "attachment; filename=\"" + path.substring(path.lastIndexOf('/') + 1) + "\""
                            ))
                            .build()
            );
        } catch (Exception e) {
            log.error("Erreur lors de la génération du lien de téléchargement de la facture [path={}]", path, e);
            throw new IllegalStateException("Impossible de préparer le téléchargement de la facture", e);
        }
    }
}
