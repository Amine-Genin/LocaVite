package ma.locavite.reservation.service;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import ma.locavite.reservation.domain.CautionTransaction;
import ma.locavite.reservation.domain.Reservation;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class CautionReceiptPdfService {

    private final PdfFontPathResolver fontPathResolver;

    public CautionReceiptPdfService(PdfFontPathResolver fontPathResolver) {
        this.fontPathResolver = fontPathResolver;
    }

    public byte[] generate(Reservation reservation, List<CautionTransaction> transactions) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfDocument pdf = new PdfDocument(new PdfWriter(output));
            Document document = new Document(pdf, PageSize.A4);
            document.setFont(PdfFontFactory.createFont(
                    fontPathResolver.resolve(), PdfEncodings.IDENTITY_H,
                    PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED));
            document.add(new Paragraph("LocaVite - Recu de caution").setBold().setFontSize(18));
            document.add(new Paragraph("Reservation : " + reservation.getCode()));
            document.add(new Paragraph("Montant de la caution : "
                    + reservation.getCaution().toPlainString() + " MAD"));
            document.add(new Paragraph("Statut : " + reservation.getCautionStatut()));

            Table table = new Table(UnitValue.createPercentArray(new float[]{25, 25, 20, 30}))
                    .useAllAvailableWidth();
            table.addHeaderCell("Date");
            table.addHeaderCell("Operation");
            table.addHeaderCell("Statut");
            table.addHeaderCell("Montant (MAD)");
            for (CautionTransaction transaction : transactions) {
                table.addCell(transaction.getDate().toString());
                table.addCell(transaction.getType().name());
                table.addCell(transaction.getStatut().name());
                table.addCell(transaction.getMontant().toPlainString());
            }
            document.add(table);
            for (CautionTransaction transaction : transactions) {
                if (transaction.getNote() != null && !transaction.getNote().isBlank()) {
                    document.add(new Paragraph("Note : " + transaction.getNote()));
                }
            }
            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Impossible de generer le recu PDF de caution", exception);
        }
    }
}
