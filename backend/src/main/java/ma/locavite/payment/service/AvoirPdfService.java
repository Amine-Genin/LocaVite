package ma.locavite.payment.service;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import ma.locavite.auth.domain.User;
import ma.locavite.document.service.MinioService;
import ma.locavite.payment.domain.Payment;
import ma.locavite.payment.domain.Refund;
import ma.locavite.reservation.config.ContractProperties;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.service.PdfFontPathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AvoirPdfService {

    private static final Logger log = LoggerFactory.getLogger(AvoirPdfService.class);
    private static final BigDecimal VAT_DIVISOR = new BigDecimal("1.20");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneOffset.UTC);
    private static final DeviceRgb BRAND_BLUE = new DeviceRgb(22, 111, 160);

    private final PdfFontPathResolver fontPathResolver;
    private final MinioService minioService;
    private final Map<String, byte[]> generatedAvoirs = new ConcurrentHashMap<>();

    public AvoirPdfService() {
        this(new PdfFontPathResolver(new ContractProperties()), null);
    }

    @Autowired
    public AvoirPdfService(
            @Autowired(required = false) PdfFontPathResolver fontPathResolver,
            @Autowired(required = false) MinioService minioService) {
        this.fontPathResolver = fontPathResolver != null ? fontPathResolver : new PdfFontPathResolver(new ContractProperties());
        this.minioService = minioService;
    }

    public byte[] generate(
            Refund refund,
            Payment payment,
            Reservation reservation,
            User client,
            String avoirNumero) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfDocument pdf = new PdfDocument(new PdfWriter(output));
            Document document = new Document(pdf, PageSize.A4);
            PdfFont font = PdfFontFactory.createFont(
                    fontPathResolver.resolve(), PdfEncodings.IDENTITY_H,
                    PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
            document.setFont(font);
            document.setFontSize(10);

            document.add(new Paragraph(InvoiceService.COMPANY_NAME + " - Avoir de remboursement")
                    .setFont(font).setBold().setFontSize(16).setFontColor(BRAND_BLUE));
            document.add(new Paragraph(
                    "ICE : " + InvoiceService.COMPANY_ICE + " | RC : " + InvoiceService.COMPANY_RC +
                    " | IF : " + InvoiceService.COMPANY_IF + " | Patente : " + InvoiceService.COMPANY_PATENTE)
                    .setFont(font).setFontSize(9));
            document.add(new Paragraph(
                    "Adresse : " + InvoiceService.COMPANY_ADDRESS + " | Téléphone : " + InvoiceService.COMPANY_PHONE)
                    .setFont(font).setFontSize(9));
            document.add(new LineSeparator(new SolidLine(1.0f)).setFontColor(BRAND_BLUE));

            String effectiveAvoirNum = (avoirNumero != null && !avoirNumero.isBlank())
                    ? avoirNumero
                    : "AV-" + payment.getCode() + "-" + (refund.getId() != null ? refund.getId() : "1");
            Instant dateOp = refund.getDate() != null ? refund.getDate() : Instant.now();

            document.add(new Paragraph("AVOIR DE REMBOURSEMENT N° : " + effectiveAvoirNum)
                    .setFont(font).setBold().setFontSize(13).setFontColor(BRAND_BLUE).setMarginTop(8));
            document.add(new Paragraph("Date d'émission : " + DATE_FORMATTER.format(dateOp))
                    .setFont(font));

            String clientName = client != null
                    ? ((client.getPrenom() != null ? client.getPrenom() : "") + " " + (client.getNom() != null ? client.getNom() : "")).trim()
                    : "Client #" + payment.getUserId();
            String reservationCode = reservation != null && reservation.getCode() != null
                    ? reservation.getCode()
                    : "RES-" + payment.getReservationId();
            String devise = payment.getDevise() != null ? payment.getDevise() : "MAD";

            document.add(new Paragraph("Client : " + clientName).setFont(font).setMarginTop(6));
            document.add(new Paragraph("Paiement concerné : " + payment.getCode() + " (Statut : " + payment.getStatut() + ")").setFont(font));
            document.add(new Paragraph("Réservation : " + reservationCode).setFont(font));
            document.add(new Paragraph("Méthode de remboursement : " + refund.getMethode()).setFont(font));
            if (refund.getReference() != null) {
                document.add(new Paragraph("Référence transaction remboursement : " + refund.getReference()).setFont(font));
            }
            document.add(new Paragraph("Motif du remboursement : " + refund.getRaison()).setFont(font));

            BigDecimal montantTtc = refund.getMontant().setScale(2, RoundingMode.HALF_UP);
            BigDecimal montantHt = montantTtc.divide(VAT_DIVISOR, 2, RoundingMode.HALF_UP);
            BigDecimal montantTva = montantTtc.subtract(montantHt).setScale(2, RoundingMode.HALF_UP);

            Table table = new Table(UnitValue.createPercentArray(new float[]{35, 15, 15, 15, 20}))
                    .useAllAvailableWidth()
                    .setMarginTop(10);

            table.addHeaderCell(new Paragraph("Description").setFont(font).setBold());
            table.addHeaderCell(new Paragraph("Paiement initial").setFont(font).setBold());
            table.addHeaderCell(new Paragraph("Avoir HT").setFont(font).setBold());
            table.addHeaderCell(new Paragraph("TVA 20%").setFont(font).setBold());
            table.addHeaderCell(new Paragraph("Montant Remboursé TTC").setFont(font).setBold());

            table.addCell(new Paragraph("Remboursement paiement " + payment.getCode() + " - " + refund.getRaison()).setFont(font));
            table.addCell(new Paragraph(payment.getMontant().setScale(2, RoundingMode.HALF_UP).toPlainString() + " " + devise).setFont(font));
            table.addCell(new Paragraph(montantHt.toPlainString() + " " + devise).setFont(font));
            table.addCell(new Paragraph(montantTva.toPlainString() + " " + devise).setFont(font));
            table.addCell(new Paragraph(montantTtc.toPlainString() + " " + devise).setFont(font));

            document.add(table);

            document.add(new Paragraph(
                    "Total Avoir HT : " + montantHt.toPlainString() + " " + devise +
                    " | TVA 20% : " + montantTva.toPlainString() + " " + devise +
                    " | Total Remboursé TTC : " + montantTtc.toPlainString() + " " + devise)
                    .setFont(font).setBold().setMarginTop(8));
            document.add(new Paragraph("Statut du remboursement : " + refund.getStatut())
                    .setFont(font).setMarginTop(4));

            document.close();
            return output.toByteArray();
        } catch (Exception e) {
            log.error("Erreur lors de la génération de l'avoir PDF [paymentCode={}]", payment.getCode(), e);
            throw new IllegalStateException("Impossible de générer l'avoir PDF de remboursement", e);
        }
    }

    public String store(String avoirPath, byte[] pdfBytes) {
        if (avoirPath != null && pdfBytes != null) {
            generatedAvoirs.put(avoirPath, pdfBytes);
            if (minioService != null) {
                try {
                    minioService.putInvoice(avoirPath, pdfBytes);
                } catch (Exception e) {
                    log.debug("Stockage MinIO indisponible pour l'avoir {}, conservé en mémoire", avoirPath);
                }
            }
        }
        return avoirPath;
    }

    public byte[] load(String avoirPath) {
        if (avoirPath == null) {
            return null;
        }
        if (minioService != null) {
            try {
                byte[] fromMinio = minioService.getInvoice(avoirPath);
                if (fromMinio != null && fromMinio.length > 0) {
                    return fromMinio;
                }
            } catch (Exception ignored) {
            }
        }
        return generatedAvoirs.get(avoirPath);
    }
}
