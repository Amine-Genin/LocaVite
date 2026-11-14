package ma.locavite.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.ibm.icu.text.ArabicShaping;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfAConformanceLevel;
import com.itextpdf.kernel.pdf.PdfDocumentInfo;
import com.itextpdf.kernel.pdf.PdfOutputIntent;
import com.itextpdf.kernel.pdf.PdfString;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.BaseDirection;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.pdfa.PdfADocument;
import jakarta.mail.internet.MimeMessage;
import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.document.service.MinioService;
import ma.locavite.mail.domain.EmailLog;
import ma.locavite.mail.repository.EmailLogRepository;
import ma.locavite.payment.domain.Invoice;
import ma.locavite.payment.domain.Payment;
import ma.locavite.payment.domain.PaymentMethod;
import ma.locavite.payment.domain.PaymentStatus;
import ma.locavite.payment.exception.InvalidPaymentStatusException;
import ma.locavite.payment.exception.PaymentNotFoundException;
import ma.locavite.payment.repository.InvoiceRepository;
import ma.locavite.payment.repository.PaymentRepository;
import ma.locavite.reservation.config.ContractProperties;
import ma.locavite.reservation.domain.ContractLanguage;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.repository.ReservationRepository;
import ma.locavite.reservation.service.PdfFontPathResolver;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.repository.VehicleRepository;
import ma.locavite.vehicle.service.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Transactional
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);
    private static final BigDecimal VAT_DIVISOR = new BigDecimal("1.20");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneOffset.UTC);
    private static final DeviceRgb BRAND_BLUE = new DeviceRgb(22, 111, 160);

    public static final String COMPANY_NAME = "LocaVite SARL";
    public static final String COMPANY_ICE = "002584963000084";
    public static final String COMPANY_RC = "458921";
    public static final String COMPANY_IF = "50412896";
    public static final String COMPANY_PATENTE = "34298115";
    public static final String COMPANY_ADDRESS = "120 Boulevard Zerktouni, 20000 Casablanca, Maroc";
    public static final String COMPANY_PHONE = "+212 5 22 00 00 00";
    public static final String INVOICE_TEMPLATE = "invoice-template";

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final MinioService minioService;
    private final CurrentUserService currentUserService;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailLogRepository emailLogRepository;
    private final ContractProperties contractProperties;
    private final PdfFontPathResolver fontPathResolver;
    private final ObjectMapper objectMapper;

    private final AtomicLong fallbackSequence = new AtomicLong(0);
    private final Map<String, byte[]> generatedPdfs = new ConcurrentHashMap<>();

    @Autowired
    public InvoiceService(
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            ReservationRepository reservationRepository,
            UserRepository userRepository,
            VehicleRepository vehicleRepository,
            MinioService minioService,
            CurrentUserService currentUserService,
            JavaMailSender mailSender,
            @Autowired(required = false) TemplateEngine templateEngine,
            @Autowired(required = false) EmailLogRepository emailLogRepository,
            @Autowired(required = false) ContractProperties contractProperties,
            @Autowired(required = false) PdfFontPathResolver fontPathResolver,
            @Autowired(required = false) ObjectMapper objectMapper) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.minioService = minioService;
        this.currentUserService = currentUserService;
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.emailLogRepository = emailLogRepository;
        this.contractProperties = contractProperties != null ? contractProperties : new ContractProperties();
        this.fontPathResolver = fontPathResolver != null ? fontPathResolver : new PdfFontPathResolver(this.contractProperties);
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public Invoice generate(Payment payment) {
        return generate(payment, ContractLanguage.FR);
    }

    public Invoice generate(Payment payment, ContractLanguage language) {
        if (payment == null) {
            throw new IllegalArgumentException("Le paiement ne peut pas être nul");
        }
        if (payment.getStatut() != PaymentStatus.SUCCESS) {
            throw new InvalidPaymentStatusException(
                    "La facture ne peut être générée que pour un paiement au statut SUCCESS (statut actuel : " + payment.getStatut() + ")"
            );
        }

        ContractLanguage effectiveLang = language != null ? language : ContractLanguage.FR;

        if (payment.getId() != null) {
            Optional<Invoice> existing = invoiceRepository.findByPaymentId(payment.getId());
            if (existing.isPresent()) {
                Invoice invoice = existing.get();
                if (effectiveLang == ContractLanguage.AR) {
                    Reservation reservation = resolveReservation(payment);
                    User client = resolveClient(payment, reservation);
                    Vehicle vehicle = resolveVehicle(reservation);
                    byte[] arabicPdf = buildPdfA3(invoice, payment, reservation, client, vehicle, ContractLanguage.AR);
                    generatedPdfs.put(invoice.getCheminPdf() + ":AR", arabicPdf);
                }
                return invoice;
            }
        }

        Instant emissionDate = Instant.now();
        int year = ZonedDateTime.ofInstant(emissionDate, ZoneOffset.UTC).getYear();
        long sequenceNumber = nextSequenceValue();
        String numero = String.format("FA-%d-%04d", year, sequenceNumber);

        BigDecimal montantTTC = payment.getMontant().setScale(2, RoundingMode.HALF_UP);
        BigDecimal montantHT = montantTTC.divide(VAT_DIVISOR, 2, RoundingMode.HALF_UP);
        BigDecimal montantTVA = montantTTC.subtract(montantHT).setScale(2, RoundingMode.HALF_UP);

        String cheminPdf = "invoices/" + year + "/" + numero + ".pdf";

        Invoice invoice = new Invoice();
        invoice.setNumero(numero);
        invoice.setPaymentId(payment.getId());
        invoice.setMontantHT(montantHT);
        invoice.setMontantTVA(montantTVA);
        invoice.setMontantTTC(montantTTC);
        invoice.setCheminPdf(cheminPdf);
        invoice.setDateEmission(emissionDate);

        Reservation reservation = resolveReservation(payment);
        User client = resolveClient(payment, reservation);
        Vehicle vehicle = resolveVehicle(reservation);

        byte[] pdfBytes = buildPdfA3(invoice, payment, reservation, client, vehicle, effectiveLang);
        generatedPdfs.put(cheminPdf, pdfBytes);
        generatedPdfs.put(cheminPdf + ":" + effectiveLang.name(), pdfBytes);

        if (minioService != null) {
            minioService.putInvoice(cheminPdf, pdfBytes);
        }

        Invoice saved = invoiceRepository.save(invoice);
        sendInvoiceEmail(client, reservation, payment, saved, pdfBytes, effectiveLang);

        log.info("Facture {} générée avec succès pour le paiement {} [montantTTC={} {}]",
                saved.getNumero(), payment.getCode(), saved.getMontantTTC(), payment.getDevise());
        return saved;
    }

    @Transactional
    public byte[] downloadInvoicePdf(Long paymentId) {
        return downloadInvoicePdf(paymentId, ContractLanguage.FR);
    }

    @Transactional
    public byte[] downloadInvoicePdf(Long paymentId, ContractLanguage language) {
        Long currentUserId = getCurrentUserId();

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        verifyDownloadAccess(payment, currentUserId);

        if (payment.getStatut() != PaymentStatus.SUCCESS) {
            throw new InvalidPaymentStatusException("La facture PDF n'est disponible que pour un paiement confirmé (SUCCESS)");
        }

        ContractLanguage effectiveLang = language != null ? language : ContractLanguage.FR;
        Invoice invoice = invoiceRepository.findByPaymentId(paymentId)
                .orElseGet(() -> generate(payment, effectiveLang));

        if (effectiveLang == ContractLanguage.AR) {
            String arKey = invoice.getCheminPdf() + ":AR";
            byte[] cachedAr = generatedPdfs.get(arKey);
            if (cachedAr != null && cachedAr.length > 0) {
                return cachedAr;
            }
            Reservation reservation = resolveReservation(payment);
            User client = resolveClient(payment, reservation);
            Vehicle vehicle = resolveVehicle(reservation);
            byte[] arPdf = buildPdfA3(invoice, payment, reservation, client, vehicle, ContractLanguage.AR);
            generatedPdfs.put(arKey, arPdf);
            return arPdf;
        }

        if (minioService != null) {
            try {
                byte[] fromMinio = minioService.getInvoice(invoice.getCheminPdf());
                if (fromMinio != null && fromMinio.length > 0) {
                    return fromMinio;
                }
            } catch (Exception e) {
                log.debug("Lecture MinIO indisponible pour {}, utilisation du cache ou régénération", invoice.getCheminPdf());
            }
        }

        byte[] cached = generatedPdfs.get(invoice.getCheminPdf());
        if (cached != null && cached.length > 0) {
            return cached;
        }

        Reservation reservation = resolveReservation(payment);
        User client = resolveClient(payment, reservation);
        Vehicle vehicle = resolveVehicle(reservation);
        byte[] regenerated = buildPdfA3(invoice, payment, reservation, client, vehicle, effectiveLang);
        generatedPdfs.put(invoice.getCheminPdf(), regenerated);
        return regenerated;
    }

    @Transactional(readOnly = true)
    public String getInvoiceFilename(Long paymentId) {
        return invoiceRepository.findByPaymentId(paymentId)
                .map(inv -> "facture-" + inv.getNumero() + ".pdf")
                .orElse("facture-paiement-" + paymentId + ".pdf");
    }

    private void verifyDownloadAccess(Payment payment, Long currentUserId) {
        boolean isOwner = payment.getUserId() != null && payment.getUserId().equals(currentUserId);
        boolean isAdmin = currentUserService != null && currentUserService.isAdmin();
        boolean isAgency = currentUserService != null && currentUserService.isAgence() && isVehicleAgency(payment, currentUserId);

        if (!isOwner && !isAdmin && !isAgency) {
            throw new AccessDeniedException("Accès refusé : vous n'êtes pas autorisé à télécharger cette facture");
        }
    }

    private boolean isVehicleAgency(Payment payment, Long currentUserId) {
        if (reservationRepository == null || vehicleRepository == null || payment.getReservationId() == null) {
            return false;
        }
        return reservationRepository.findById(payment.getReservationId())
                .flatMap(res -> vehicleRepository.findById(res.getVehicleId()))
                .map(Vehicle::getAgenceId)
                .map(agenceId -> agenceId.equals(currentUserId))
                .orElse(false);
    }

    private Long getCurrentUserId() {
        if (currentUserService == null) {
            throw new AccessDeniedException("Utilisateur non authentifié");
        }
        return currentUserService.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur non authentifié"));
    }

    private long nextSequenceValue() {
        if (invoiceRepository != null) {
            try {
                Long seq = invoiceRepository.getNextSequenceValue();
                if (seq != null && seq > 0) {
                    fallbackSequence.updateAndGet(curr -> Math.max(curr, seq));
                    return seq;
                }
            } catch (Exception e) {
                log.debug("Séquence SQL non disponible dans ce contexte, utilisation du compteur atomique : {}", e.getMessage());
            }
        }
        return fallbackSequence.incrementAndGet();
    }

    private Reservation resolveReservation(Payment payment) {
        if (reservationRepository != null && payment.getReservationId() != null) {
            return reservationRepository.findById(payment.getReservationId()).orElse(null);
        }
        return null;
    }

    private User resolveClient(Payment payment, Reservation reservation) {
        Long userId = payment.getUserId() != null ? payment.getUserId()
                : (reservation != null ? reservation.getUserId() : null);
        if (userRepository != null && userId != null) {
            return userRepository.findById(userId).orElse(null);
        }
        return null;
    }

    private Vehicle resolveVehicle(Reservation reservation) {
        if (vehicleRepository != null && reservation != null && reservation.getVehicleId() != null) {
            return vehicleRepository.findById(reservation.getVehicleId()).orElse(null);
        }
        return null;
    }

    private byte[] buildPdfA3(
            Invoice invoice,
            Payment payment,
            Reservation reservation,
            User client,
            Vehicle vehicle,
            ContractLanguage language) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(output);
            PdfOutputIntent outputIntent = new PdfOutputIntent(
                    "Custom", "", "http://www.color.org", "sRGB IEC61966-2.1",
                    new ByteArrayInputStream(ICC_Profile.getInstance(ColorSpace.CS_sRGB).getData())
            );
            PdfADocument pdf = new PdfADocument(writer, PdfAConformanceLevel.PDF_A_3U, outputIntent);
            PdfDocumentInfo info = pdf.getDocumentInfo();
            info.setTitle("Facture LocaVite " + invoice.getNumero());
            info.setAuthor(COMPANY_NAME);
            info.setSubject("Facture PDF/A-3 - " + invoice.getNumero());
            pdf.getCatalog().setLang(new PdfString(language == ContractLanguage.AR ? "ar-MA" : "fr-MA"));

            Document document = new Document(pdf, PageSize.A4);
            PdfFont font = PdfFontFactory.createFont(
                    fontPathResolver.resolve(), PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED
            );
            document.setFont(font);
            document.setFontSize(10);

            BaseDirection dir = language == ContractLanguage.AR ? BaseDirection.RIGHT_TO_LEFT : BaseDirection.LEFT_TO_RIGHT;

            // 1. En-tête LocaVite + ICE + RC + IF + Patente + adresse + téléphone
            document.add(new Paragraph(COMPANY_NAME)
                    .setFont(font).setFontSize(16).setBold().setFontColor(BRAND_BLUE));
            document.add(new Paragraph(
                    "ICE : " + COMPANY_ICE + " | RC : " + COMPANY_RC +
                    " | IF : " + COMPANY_IF + " | Patente : " + COMPANY_PATENTE)
                    .setFont(font).setFontSize(9));
            document.add(new Paragraph(
                    "Adresse : " + COMPANY_ADDRESS + " | Téléphone : " + COMPANY_PHONE)
                    .setFont(font).setFontSize(9));
            document.add(new LineSeparator(new SolidLine(1.0f)).setFontColor(BRAND_BLUE));

            // 2. Titre bilingue FR/AR + Numéro séquentiel + Date d'émission
            String arTitle = shapeArabic("فاتورة ضريبية");
            document.add(new Paragraph("FACTURE / " + arTitle + " N° : " + invoice.getNumero())
                    .setFont(font).setFontSize(14).setBold().setFontColor(BRAND_BLUE).setMarginTop(8));

            String dateFormatted = DATE_FORMATTER.format(invoice.getDateEmission());
            document.add(new Paragraph("Date d'émission / " + shapeArabic("تاريخ الإصدار") + " : " + dateFormatted)
                    .setFont(font).setFontSize(10).setBaseDirection(dir));

            // 3. Informations client (nom, ICE si entreprise, adresse)
            String clientName = client != null
                    ? ((client.getPrenom() != null ? client.getPrenom() : "") + " " + (client.getNom() != null ? client.getNom() : "")).trim()
                    : "Client #" + payment.getUserId();
            String clientIce = extractMetadataField(payment.getMetadata(), "clientIce", "ice", "N/A (Particulier)");
            String defaultAddress = reservation != null && reservation.getLieuPriseEnCharge() != null
                    ? reservation.getLieuPriseEnCharge() + ", Maroc"
                    : "Casablanca, Maroc";
            String clientAddress = extractMetadataField(payment.getMetadata(), "clientAddress", "adresse", defaultAddress);

            document.add(new Paragraph("Informations Client / " + shapeArabic("معلومات الزبون"))
                    .setFont(font).setFontSize(11).setBold().setFontColor(BRAND_BLUE).setMarginTop(8));
            document.add(new Paragraph("Nom du client : " + clientName)
                    .setFont(font).setFontSize(10));
            document.add(new Paragraph("ICE Client (si entreprise) : " + clientIce)
                    .setFont(font).setFontSize(10));
            document.add(new Paragraph("Adresse client : " + clientAddress)
                    .setFont(font).setFontSize(10));

            // 4. Détail : description, quantité, prix unitaire, TVA 20%, total HT, total TTC
            long quantity = calculateQuantity(reservation);
            BigDecimal unitPriceHt = invoice.getMontantHT()
                    .divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
            String reservationCode = reservation != null && reservation.getCode() != null
                    ? reservation.getCode()
                    : "RES-" + payment.getReservationId();
            String vehicleLabel = vehicle != null
                    ? ((vehicle.getBrand() != null && vehicle.getBrand().getName() != null ? vehicle.getBrand().getName() + " " : "")
                       + (vehicle.getModele() != null ? vehicle.getModele() : "")).trim()
                    : "Véhicule";
            String description = "Location de véhicule (" + vehicleLabel + ") - Réservation " + reservationCode;

            document.add(new Paragraph("Détail de la facturation / " + shapeArabic("تفاصيل الفاتورة"))
                    .setFont(font).setFontSize(11).setBold().setFontColor(BRAND_BLUE).setMarginTop(8));

            Table table = new Table(UnitValue.createPercentArray(new float[]{32, 10, 15, 13, 15, 15}))
                    .useAllAvailableWidth()
                    .setMarginTop(4);

            table.addHeaderCell(new Paragraph("Description").setFont(font).setBold());
            table.addHeaderCell(new Paragraph("Quantité").setFont(font).setBold());
            table.addHeaderCell(new Paragraph("Prix unitaire HT").setFont(font).setBold());
            table.addHeaderCell(new Paragraph("TVA 20%").setFont(font).setBold());
            table.addHeaderCell(new Paragraph("Total HT").setFont(font).setBold());
            table.addHeaderCell(new Paragraph("Total TTC").setFont(font).setBold());

            String devise = payment.getDevise() != null ? payment.getDevise() : "MAD";
            table.addCell(new Paragraph(description).setFont(font));
            table.addCell(new Paragraph(String.valueOf(quantity)).setFont(font));
            table.addCell(new Paragraph(unitPriceHt.toPlainString() + " " + devise).setFont(font));
            table.addCell(new Paragraph(invoice.getMontantTVA().toPlainString() + " " + devise + " (20%)").setFont(font));
            table.addCell(new Paragraph(invoice.getMontantHT().toPlainString() + " " + devise).setFont(font));
            table.addCell(new Paragraph(invoice.getMontantTTC().toPlainString() + " " + devise).setFont(font));

            document.add(table);

            // Récapitulatif HT / TVA 20% / TTC
            document.add(new Paragraph(
                    "Total HT : " + invoice.getMontantHT().toPlainString() + " " + devise +
                    " | TVA 20% : " + invoice.getMontantTVA().toPlainString() + " " + devise +
                    " | Total TTC : " + invoice.getMontantTTC().toPlainString() + " " + devise)
                    .setFont(font).setFontSize(10).setBold().setMarginTop(6));

            // 5. Mode de paiement
            String methodLabel = formatPaymentMethod(payment.getMethode());
            document.add(new Paragraph(
                    "Mode de paiement / " + shapeArabic("طريقة الدفع") + " : " +
                    payment.getMethode().name() + " - " + methodLabel +
                    " (Paiement : " + payment.getCode() + ")")
                    .setFont(font).setFontSize(10).setMarginTop(6));

            // 6. Mentions légales
            document.add(new Paragraph("Mentions légales / " + shapeArabic("مقتضيات قانونية"))
                    .setFont(font).setFontSize(10).setBold().setFontColor(BRAND_BLUE).setMarginTop(8));
            document.add(new Paragraph(
                    "Facture conforme aux dispositions de l'article 145 du Code Général des Impôts (CGI) marocain. " +
                    "Format d'archivage légal PDF/A-3. Taux de TVA légal de 20% appliqué. " +
                    "Numérotation séquentielle chronologique sans rupture.")
                    .setFont(font).setFontSize(8).setFontColor(ColorConstants.DARK_GRAY));

            // 7. QR code de vérification
            String baseUrl = contractProperties.getPublicBaseUrl() != null
                    ? contractProperties.getPublicBaseUrl().replaceAll("/+$", "")
                    : "http://localhost:8081";
            String verificationData = baseUrl + "/api/payments/" + payment.getId()
                    + "/invoice?verify=" + invoice.getNumero()
                    + "&ttc=" + invoice.getMontantTTC().toPlainString();

            byte[] qrBytes = createQrCode(verificationData);
            Image qrImage = new Image(ImageDataFactory.create(qrBytes))
                    .scaleToFit(75, 75)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER)
                    .setMarginTop(8);
            document.add(qrImage);
            document.add(new Paragraph("QR code de vérification - " + invoice.getNumero())
                    .setFont(font).setFontSize(8).setTextAlignment(TextAlignment.CENTER));

            document.close();
            return output.toByteArray();
        } catch (Exception e) {
            log.error("Erreur lors de la génération de la facture PDF/A-3 [numero={}]", invoice.getNumero(), e);
            throw new IllegalStateException("Impossible de générer la facture PDF/A-3", e);
        }
    }

    private long calculateQuantity(Reservation reservation) {
        if (reservation != null && reservation.getDateDebut() != null && reservation.getDateFin() != null) {
            long days = ChronoUnit.DAYS.between(reservation.getDateDebut(), reservation.getDateFin());
            return Math.max(1L, days);
        }
        return 1L;
    }

    private String extractMetadataField(String metadataJson, String key1, String key2, String defaultValue) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return defaultValue;
        }
        try {
            JsonNode node = objectMapper.readTree(metadataJson);
            if (node.hasNonNull(key1) && !node.get(key1).asText().isBlank()) {
                return node.get(key1).asText();
            }
            if (node.hasNonNull(key2) && !node.get(key2).asText().isBlank()) {
                return node.get(key2).asText();
            }
        } catch (Exception ignored) {
            // Ignorer si metadata n'est pas du JSON valide
        }
        return defaultValue;
    }

    private String formatPaymentMethod(PaymentMethod method) {
        if (method == null) {
            return "Non spécifié";
        }
        return switch (method) {
            case CMI -> "Carte bancaire en ligne (CMI)";
            case CASH_DELIVERY -> "Paiement en espèces à la livraison";
            case CASH_AGENCY -> "Paiement en espèces en agence";
            case BANK_TRANSFER -> "Virement bancaire";
        };
    }

    private String shapeArabic(String value) {
        try {
            return new ArabicShaping(ArabicShaping.LETTERS_SHAPE).shape(value);
        } catch (Exception e) {
            return value;
        }
    }

    private byte[] createQrCode(String value) throws Exception {
        BitMatrix matrix = new QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, 220, 220);
        BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < matrix.getWidth(); x++) {
            for (int y = 0; y < matrix.getHeight(); y++) {
                image.setRGB(x, y, matrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private void sendInvoiceEmail(
            User client,
            Reservation reservation,
            Payment payment,
            Invoice invoice,
            byte[] pdfBytes,
            ContractLanguage language) {
        if (mailSender == null) {
            return;
        }
        String recipient = client != null && client.getEmail() != null ? client.getEmail() : "client@locavite.ma";
        String subject = "Votre facture LocaVite - " + invoice.getNumero();
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            if (mimeMessage == null) {
                return;
            }
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom("noreply@locavite.ma");
            helper.setTo(recipient);
            helper.setSubject(subject);

            String htmlBody = renderInvoiceHtml(client, reservation, payment, invoice, language);
            helper.setText(htmlBody, true);
            helper.addAttachment("facture-" + invoice.getNumero() + ".pdf",
                    new ByteArrayResource(pdfBytes), "application/pdf");

            mailSender.send(mimeMessage);
            saveEmailLog(reservation != null ? reservation.getId() : null, recipient, subject, "SENT", null);
            log.info("Facture {} envoyée par email à {}", invoice.getNumero(), recipient);
        } catch (Exception e) {
            log.warn("Impossible d'envoyer l'email de la facture {} à {} : {}", invoice.getNumero(), recipient, e.getMessage());
            saveEmailLog(reservation != null ? reservation.getId() : null, recipient, subject, "FAILED", e.getMessage());
        }
    }

    private String renderInvoiceHtml(
            User client,
            Reservation reservation,
            Payment payment,
            Invoice invoice,
            ContractLanguage language) {
        String clientName = client != null
                ? ((client.getPrenom() != null ? client.getPrenom() : "") + " " + (client.getNom() != null ? client.getNom() : "")).trim()
                : "Client";
        long quantity = calculateQuantity(reservation);
        BigDecimal unitPriceHt = invoice.getMontantHT().divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
        String reservationCode = reservation != null && reservation.getCode() != null
                ? reservation.getCode()
                : "RES-" + payment.getReservationId();

        if (templateEngine != null) {
            try {
                Context ctx = new Context(language == ContractLanguage.AR ? Locale.forLanguageTag("ar") : Locale.FRENCH);
                ctx.setVariable("lang", language.name().toLowerCase(Locale.ROOT));
                ctx.setVariable("isRtl", language == ContractLanguage.AR);
                ctx.setVariable("invoice", invoice);
                ctx.setVariable("payment", payment);
                ctx.setVariable("dateEmissionFormatted", DATE_FORMATTER.format(invoice.getDateEmission()));
                ctx.setVariable("paymentMethodLabel", formatPaymentMethod(payment.getMethode()));
                ctx.setVariable("clientName", clientName);
                ctx.setVariable("clientIce", extractMetadataField(payment.getMetadata(), "clientIce", "ice", "N/A (Particulier)"));
                ctx.setVariable("clientAddress", extractMetadataField(payment.getMetadata(), "clientAddress", "adresse", "Casablanca, Maroc"));
                ctx.setVariable("lineDescription", "Location de véhicule - Réservation " + reservationCode);
                ctx.setVariable("quantity", quantity);
                ctx.setVariable("unitPriceHt", unitPriceHt.toPlainString());
                String rendered = templateEngine.process(INVOICE_TEMPLATE, ctx);
                if (rendered != null && !rendered.isBlank()) {
                    return rendered;
                }
            } catch (Exception e) {
                log.debug("Rendu Thymeleaf de invoice-template non utilisé, fallback HTML : {}", e.getMessage());
            }
        }

        return "<html><body><h2>LocaVite - Facture " + invoice.getNumero() + "</h2>"
                + "<p>Bonjour " + clientName + ",</p>"
                + "<p>Veuillez trouver ci-joint votre facture <strong>" + invoice.getNumero() + "</strong> "
                + "d'un montant de <strong>" + invoice.getMontantTTC().toPlainString() + " " + payment.getDevise() + " TTC</strong> "
                + "(HT : " + invoice.getMontantHT().toPlainString() + ", TVA 20% : " + invoice.getMontantTVA().toPlainString() + ").</p>"
                + "</body></html>";
    }

    private void saveEmailLog(Long reservationId, String recipient, String subject, String status, String errorMessage) {
        if (emailLogRepository != null) {
            try {
                EmailLog emailLog = new EmailLog(reservationId, recipient, subject, INVOICE_TEMPLATE, status, errorMessage, 1);
                emailLogRepository.save(emailLog);
            } catch (Exception ignored) {
            }
        }
    }
}
