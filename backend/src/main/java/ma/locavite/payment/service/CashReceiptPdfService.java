package ma.locavite.payment.service;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import ma.locavite.payment.domain.Payment;
import ma.locavite.payment.domain.PaymentMethod;
import ma.locavite.reservation.config.ContractProperties;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.service.PdfFontPathResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Instant;

@Service
public class CashReceiptPdfService {

    private final PdfFontPathResolver fontPathResolver;

    public CashReceiptPdfService() {
        this(new PdfFontPathResolver(new ContractProperties()));
    }

    @Autowired
    public CashReceiptPdfService(PdfFontPathResolver fontPathResolver) {
        this.fontPathResolver = fontPathResolver;
    }

    public byte[] generate(Payment payment, Reservation reservation) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfDocument pdf = new PdfDocument(new PdfWriter(output));
            Document document = new Document(pdf, PageSize.A4);
            document.setFont(PdfFontFactory.createFont(
                    fontPathResolver.resolve(), PdfEncodings.IDENTITY_H,
                    PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED));

            document.add(new Paragraph("LocaVite - Recu de paiement en especes (Ticket de caisse)")
                    .setBold()
                    .setFontSize(14));
            document.add(new Paragraph("Ticket / Paiement : " + payment.getCode()));
            document.add(new Paragraph("Reservation : " + reservation.getCode()));
            document.add(new Paragraph("Methode : " + payment.getMethode() + " - " + formatMethodLabel(payment.getMethode())));
            document.add(new Paragraph("Montant paye : " + payment.getMontant().toPlainString() + " " + payment.getDevise()));
            document.add(new Paragraph("Statut : " + payment.getStatut()));
            Instant paidAt = payment.getPaidAt() != null ? payment.getPaidAt() : Instant.now();
            document.add(new Paragraph("Date de paiement : " + paidAt));

            Table table = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25}))
                    .useAllAvailableWidth();
            table.addHeaderCell("Code Paiement");
            table.addHeaderCell("Reservation");
            table.addHeaderCell("Mode");
            table.addHeaderCell("Montant (" + payment.getDevise() + ")");

            table.addCell(payment.getCode());
            table.addCell(reservation.getCode());
            table.addCell(payment.getMethode().name());
            table.addCell(payment.getMontant().toPlainString());

            document.add(table);
            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Impossible de generer le recu PDF de paiement en especes", exception);
        }
    }

    private String formatMethodLabel(PaymentMethod method) {
        if (method == PaymentMethod.CASH_DELIVERY) {
            return "Paiement a la livraison du vehicule";
        }
        if (method == PaymentMethod.CASH_AGENCY) {
            return "Paiement a l'agence avant prise en charge";
        }
        return method != null ? method.name() : "";
    }
}
