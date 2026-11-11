package ma.locavite.reservation.service;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import ma.locavite.document.service.MinioService;
import ma.locavite.reservation.domain.Refund;
import ma.locavite.reservation.domain.Reservation;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class CancellationPdfService {

    private final PdfFontPathResolver fontPathResolver;
    private final MinioService minioService;

    public CancellationPdfService(PdfFontPathResolver fontPathResolver, MinioService minioService) {
        this.fontPathResolver = fontPathResolver;
        this.minioService = minioService;
    }

    public String generateAndStore(Reservation reservation, Refund refund) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfDocument pdf = new PdfDocument(new PdfWriter(output));
            Document document = new Document(pdf, PageSize.A4);
            document.setFont(PdfFontFactory.createFont(
                    fontPathResolver.resolve(), PdfEncodings.IDENTITY_H,
                    PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED));
            document.add(new Paragraph("LocaVite - Avoir d'annulation").setBold().setFontSize(18));
            document.add(new Paragraph("Réservation : " + reservation.getCode()));
            document.add(new Paragraph("Montant initial : " + reservation.getPrixTotal().toPlainString() + " MAD"));
            document.add(new Paragraph("Montant remboursé : " + refund.getMontant().toPlainString() + " MAD"));
            document.add(new Paragraph("Motif : " + refund.getRaison()));
            document.close();

            String path = reservation.getId() + "/cancel/avoir-" + reservation.getCode() + ".pdf";
            minioService.putContract(path, output.toByteArray());
            return path;
        } catch (Exception exception) {
            throw new IllegalStateException("Impossible de générer l'avoir PDF d'annulation", exception);
        }
    }
}
