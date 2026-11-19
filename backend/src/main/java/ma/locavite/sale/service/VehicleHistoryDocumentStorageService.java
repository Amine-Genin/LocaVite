package ma.locavite.sale.service;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.ServerSideEncryptionS3;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import ma.locavite.document.config.MinioProperties;
import ma.locavite.sale.exception.InvalidVehicleHistoryDocumentException;
import ma.locavite.sale.exception.VehicleHistoryStorageException;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class VehicleHistoryDocumentStorageService {

    public static final long MAX_DOCUMENT_SIZE = 5 * 1024 * 1024;
    public static final int PRESIGNED_URL_EXPIRY_SECONDS = 15 * 60;

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    private final MinioClient minioClient;
    private final MinioProperties properties;
    private final String bucket;
    private final Tika tika = new Tika();

    public VehicleHistoryDocumentStorageService(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
        this.bucket = properties.getHistoryBucket();
    }

    @PostConstruct
    public void initializeBucket() {
        if (!properties.isInitBucket()) {
            return;
        }

        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new VehicleHistoryStorageException("Impossible d'initialiser le stockage de l'historique", e);
        }
    }

    public String upload(Long saleListingId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidVehicleHistoryDocumentException("Le document ne peut pas être vide");
        }
        if (file.getSize() > MAX_DOCUMENT_SIZE) {
            throw new InvalidVehicleHistoryDocumentException("Le document dépasse la taille maximale de 5 Mo");
        }

        byte[] contents;
        String mimeType;
        try {
            contents = file.getBytes();
            mimeType = tika.detect(contents);
        } catch (IOException e) {
            throw new InvalidVehicleHistoryDocumentException("Impossible de lire le document", e);
        }
        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new InvalidVehicleHistoryDocumentException(
                    "Type de document non autorisé. Formats acceptés : PDF, JPEG, PNG");
        }

        String objectPath = "sale-listings/" + saleListingId + "/history/"
                + UUID.randomUUID() + "." + extensionFor(mimeType);
        PutObjectArgs.Builder upload = PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectPath)
                .stream(new ByteArrayInputStream(contents), contents.length, -1)
                .contentType(mimeType);
        if (properties.isSseEnabled()) {
            upload.sse(new ServerSideEncryptionS3());
        }

        try {
            minioClient.putObject(upload.build());
            return objectPath;
        } catch (Exception e) {
            throw new VehicleHistoryStorageException("Impossible d'enregistrer le justificatif", e);
        }
    }

    public String getPresignedUrl(String objectPath) {
        if (objectPath == null) {
            return null;
        }
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectPath)
                    .expiry(PRESIGNED_URL_EXPIRY_SECONDS, TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            throw new VehicleHistoryStorageException("Impossible de préparer le téléchargement du justificatif", e);
        }
    }

    public void delete(String objectPath) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectPath)
                    .build());
        } catch (Exception e) {
            throw new VehicleHistoryStorageException("Impossible de supprimer le justificatif", e);
        }
    }

    private String extensionFor(String mimeType) {
        return switch (mimeType) {
            case "application/pdf" -> "pdf";
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            default -> throw new InvalidVehicleHistoryDocumentException("Type de document non autorisé");
        };
    }
}
