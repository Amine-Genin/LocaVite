package ma.locavite.sale.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import ma.locavite.document.config.MinioProperties;
import ma.locavite.sale.exception.InvalidVehicleHistoryDocumentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleHistoryDocumentStorageServiceTest {

    @Mock
    private MinioClient minioClient;

    private VehicleHistoryDocumentStorageService storage;

    @BeforeEach
    void setUp() {
        MinioProperties properties = new MinioProperties();
        properties.setHistoryBucket("locavite-history-docs");
        properties.setInitBucket(false);
        storage = new VehicleHistoryDocumentStorageService(minioClient, properties);
    }

    @Test
    void uploadsPdfToHistoryBucketWithGeneratedPath() throws Exception {
        MockMultipartFile file = pdfFile();

        String path = storage.upload(42L, file);

        assertThat(path).startsWith("sale-listings/42/history/").endsWith(".pdf");
        ArgumentCaptor<PutObjectArgs> args = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(args.capture());
        assertThat(args.getValue().bucket()).isEqualTo("locavite-history-docs");
        assertThat(args.getValue().object()).isEqualTo(path);
        assertThat(args.getValue().contentType()).isEqualTo("application/pdf");
    }

    @Test
    void rejectsUnsupportedContentEvenWhenFileNameLooksLikePdf() {
        MockMultipartFile file = new MockMultipartFile(
                "document", "invoice.pdf", "application/pdf", "not a pdf".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> storage.upload(42L, file))
                .isInstanceOf(InvalidVehicleHistoryDocumentException.class);
    }

    @Test
    void rejectsEmptyDocuments() {
        MockMultipartFile file = new MockMultipartFile(
                "document", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> storage.upload(42L, file))
                .isInstanceOf(InvalidVehicleHistoryDocumentException.class)
                .hasMessageContaining("vide");
    }

    @Test
    void rejectsDocumentsLargerThanFiveMegabytes() {
        MockMultipartFile file = new MockMultipartFile(
                "document", "large.pdf", "application/pdf",
                new byte[(int) VehicleHistoryDocumentStorageService.MAX_DOCUMENT_SIZE + 1]);

        assertThatThrownBy(() -> storage.upload(42L, file))
                .isInstanceOf(InvalidVehicleHistoryDocumentException.class)
                .hasMessageContaining("5 Mo");
    }

    @Test
    void generatesPresignedUrlFromHistoryBucket() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("https://minio.example/history-document");

        assertThat(storage.getPresignedUrl("sale-listings/42/history/report.pdf"))
                .isEqualTo("https://minio.example/history-document");
        verify(minioClient).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
    }

    private MockMultipartFile pdfFile() {
        byte[] content = ("%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n"
                + "trailer\n<< /Root 1 0 R >>\n%%EOF").getBytes(StandardCharsets.US_ASCII);
        return new MockMultipartFile("document", "invoice.pdf", "application/pdf", content);
    }
}
