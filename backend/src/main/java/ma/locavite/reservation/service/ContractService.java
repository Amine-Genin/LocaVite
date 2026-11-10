package ma.locavite.reservation.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.ibm.icu.text.ArabicShaping;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfAConformanceLevel;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfDocumentInfo;
import com.itextpdf.kernel.pdf.PdfOutputIntent;
import com.itextpdf.kernel.pdf.PdfString;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.pdfa.PdfADocument;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.BaseDirection;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.document.domain.DocumentStatus;
import ma.locavite.document.domain.DocumentType;
import ma.locavite.document.repository.DocumentRepository;
import ma.locavite.document.service.MinioService;
import ma.locavite.reservation.config.ContractProperties;
import ma.locavite.reservation.domain.ContractLanguage;
import ma.locavite.reservation.domain.ReservationOption;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationContract;
import ma.locavite.reservation.dto.PriceQuoteResponse;
import ma.locavite.reservation.repository.ReservationContractRepository;
import ma.locavite.reservation.repository.ReservationRepository;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Map;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class ContractService {

    private static final Logger log = LoggerFactory.getLogger(ContractService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DeviceRgb BRAND_BLUE = new DeviceRgb(22, 111, 160);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final ReservationRepository reservationRepository;
    private final ReservationContractRepository contractRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final DocumentRepository documentRepository;
    private final PricingService pricingService;
    private final MinioService minioService;
    private final JavaMailSender mailSender;
    private final ContractProperties properties;
    private final PdfFontPathResolver fontPathResolver;

    public ContractService(
            ReservationRepository reservationRepository,
            ReservationContractRepository contractRepository,
            UserRepository userRepository,
            VehicleRepository vehicleRepository,
            DocumentRepository documentRepository,
            PricingService pricingService,
            MinioService minioService,
            JavaMailSender mailSender,
            ContractProperties properties,
            PdfFontPathResolver fontPathResolver) {
        this.reservationRepository = reservationRepository;
        this.contractRepository = contractRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.documentRepository = documentRepository;
        this.pricingService = pricingService;
        this.minioService = minioService;
        this.mailSender = mailSender;
        this.properties = properties;
        this.fontPathResolver = fontPathResolver;
    }

    @Transactional
    public ReservationContract generate(Reservation reservation, ContractLanguage language) {
        Optional<ReservationContract> existing =
                contractRepository.findByReservationIdAndLanguage(reservation.getId(), language);
        if (existing.isPresent()) {
            return existing.get();
        }

        User client = userRepository.findById(reservation.getUserId())
                .orElseThrow(() -> new IllegalStateException("Client introuvable pour la réservation"));
        Vehicle vehicle = vehicleRepository.findById(reservation.getVehicleId())
                .orElseThrow(() -> new IllegalStateException("Véhicule introuvable pour la réservation"));
        List<ma.locavite.document.domain.Document> documents =
                documentRepository.findByUserIdOrderByUploadedAtDesc(client.getId());

        PriceQuoteResponse quote = storedQuote(reservation, pricingService.calculate(
                vehicle, reservation.getDateDebut(), reservation.getDateFin(), reservation.getOptions()));
        String accessToken = createAccessToken();
        String digitalUrl = properties.getPublicBaseUrl().replaceAll("/+$", "")
                + "/api/reservations/" + reservation.getId() + "/contract/digital?lang="
                + language.name() + "&token=" + accessToken;
        byte[] pdf = generatePdf(reservation, client, vehicle, documents, quote, language, digitalUrl);
        String path = reservation.getId() + "/" + language.name().toLowerCase(Locale.ROOT)
                + "/contract-" + reservation.getCode() + ".pdf";
        minioService.putContract(path, pdf);

        ReservationContract contract = new ReservationContract();
        contract.setReservation(reservation);
        contract.setLanguage(language);
        contract.setObjectPath(path);
        contract.setAccessToken(accessToken);
        contract.setGeneratedAt(Instant.now());
        ReservationContract saved = contractRepository.save(contract);
        log.info("Contrat {} généré pour la réservation {}", language, reservation.getCode());
        return saved;
    }

    @Transactional
    public String getDownloadUrl(Long reservationId, Long currentUserId, ContractLanguage language) {
        ReservationContract contract = getConfirmedContract(reservationId, currentUserId, language);
        return minioService.getContractPresignedUrl(contract.getObjectPath());
    }

    @Transactional
    public String getDigitalDownloadUrl(Long reservationId, ContractLanguage language, String accessToken) {
        ReservationContract contract = contractRepository
                .findByReservationIdAndLanguageAndAccessToken(reservationId, language, accessToken)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                        "Lien numérique de contrat invalide"));
        return minioService.getContractPresignedUrl(contract.getObjectPath());
    }

    private ReservationContract getConfirmedContract(
            Long reservationId, Long currentUserId, ContractLanguage language) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ma.locavite.reservation.exception.ReservationNotFoundException(reservationId));
        if (!reservation.getUserId().equals(currentUserId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Accès refusé : ce contrat appartient à un autre client");
        }
        if (reservation.getStatut() != ma.locavite.reservation.domain.ReservationStatus.CONFIRMED
                && reservation.getStatut() != ma.locavite.reservation.domain.ReservationStatus.ACTIVE
                && reservation.getStatut() != ma.locavite.reservation.domain.ReservationStatus.COMPLETED) {
            throw new ma.locavite.reservation.exception.InvalidReservationStatusException(
                    "Le contrat est disponible après confirmation de la réservation");
        }
        return contractRepository
                .findByReservationIdAndLanguage(reservationId, language)
                .orElseGet(() -> generate(reservation, language));
    }

    public void emailContract(Reservation reservation, ReservationContract contract) {
        User client = userRepository.findById(reservation.getUserId())
                .orElseThrow(() -> new IllegalStateException("Client introuvable pour la réservation"));
        String url = minioService.getContractPresignedUrl(contract.getObjectPath());
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("noreply@locavite.ma");
            helper.setTo(client.getEmail());
            helper.setSubject("Votre contrat de location LocaVite - " + reservation.getCode());
            helper.setText("Bonjour " + client.getPrenom() + " " + client.getNom()
                    + ",\n\nVotre réservation " + reservation.getCode()
                    + " est confirmée. Votre contrat PDF est joint à cet e-mail.\n"
                    + "Lien de téléchargement sécurisé (valable 24 heures) : " + url
                    + "\n\nCordialement,\nL'équipe LocaVite");
            helper.addAttachment("contrat-" + reservation.getCode() + ".pdf",
                    new org.springframework.core.io.ByteArrayResource(
                            minioService.getContract(contract.getObjectPath())));
            mailSender.send(message);
        } catch (MessagingException | MailException exception) {
            log.error("Échec de l'envoi du contrat {} à {}", reservation.getCode(), client.getEmail(), exception);
        }
    }

    private PriceQuoteResponse storedQuote(Reservation reservation, PriceQuoteResponse calculated) {
        if (reservation.getPrixSousTotal() == null || reservation.getPrixOptions() == null
                || reservation.getPrixRemise() == null || reservation.getPrixTva() == null) {
            return calculated;
        }
        return new PriceQuoteResponse(
                reservation.getPrixSousTotal(),
                reservation.getPrixOptions(),
                reservation.getPrixRemise(),
                reservation.getCaution(),
                reservation.getPrixTva(),
                reservation.getPrixTotal(),
                calculated.jours()
        );
    }

    private byte[] generatePdf(
            Reservation reservation,
            User client,
            Vehicle vehicle,
            List<ma.locavite.document.domain.Document> documents,
            PriceQuoteResponse quote,
            ContractLanguage language,
            String digitalUrl) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(output);
            PdfOutputIntent outputIntent = new PdfOutputIntent(
                    "Custom", "", "http://www.color.org", "sRGB IEC61966-2.1",
                    new ByteArrayInputStream(ICC_Profile.getInstance(ColorSpace.CS_sRGB).getData()));
            PdfADocument pdf = new PdfADocument(writer, PdfAConformanceLevel.PDF_A_2U, outputIntent);
            PdfDocumentInfo info = pdf.getDocumentInfo();
            info.setTitle("Contrat de location " + reservation.getCode());
            info.setAuthor("LocaVite");
            pdf.getCatalog().setLang(new PdfString(localeTag(language)));

            Document document = new Document(pdf, PageSize.A4);
            PdfFont font = PdfFontFactory.createFont(
                    fontPathResolver.resolve(), PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
            document.setFont(font);
            document.setFontSize(10);
            drawBrandHeader(pdf, font);

            Translation t = Translation.forLanguage(language);
            addParagraph(document, t.title(), font, 18, BRAND_BLUE, true, language);
            addParagraph(document, t.contractNumber() + ": " + reservation.getCode(), font, 12,
                    ColorConstants.BLACK, true, language);
            addParagraph(document, t.client() + ": " + client.getPrenom() + " " + client.getNom(),
                    font, 11, ColorConstants.BLACK, true, language);

            document.add(new Paragraph(t.identityDocuments() + ": "
                    + documentStatus(documents, DocumentType.CIN, t))
                    .setFont(font).setBaseDirection(baseDirection(language)));
            document.add(new Paragraph(t.drivingPermit() + ": "
                    + documentStatus(documents, DocumentType.PERMIS, t))
                    .setFont(font).setBaseDirection(baseDirection(language)));

            addSection(document, t.vehicle(), font, language);
            document.add(new Paragraph(t.brand() + ": " + vehicle.getBrand().getName()
                    + "    " + t.model() + ": " + vehicle.getModele()
                    + "    " + t.plate() + ": " + vehicle.getMatricule())
                    .setFont(font).setBaseDirection(baseDirection(language)));

            addSection(document, t.periodAndLocations(), font, language);
            document.add(new Paragraph(t.startDate() + ": " + DATE_FORMAT.format(reservation.getDateDebut())
                    + "    " + t.pickup() + ": " + reservation.getLieuPriseEnCharge())
                    .setFont(font).setBaseDirection(baseDirection(language)));
            document.add(new Paragraph(t.endDate() + ": " + DATE_FORMAT.format(reservation.getDateFin())
                    + "    " + t.returnLocation() + ": " + reservation.getLieuRetour())
                    .setFont(font).setBaseDirection(baseDirection(language)));

            addSection(document, t.priceDetails(), font, language);
            Table priceTable = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                    .useAllAvailableWidth();
            addPriceRow(priceTable, t.rental(), quote.sousTotal(), font, language);
            String selectedOptions = reservation.getOptions().stream()
                    .map(option -> optionName(option, language))
                    .sorted()
                    .collect(java.util.stream.Collectors.joining(", "));
            addPriceRow(priceTable,
                    t.options() + (selectedOptions.isBlank() ? "" : " (" + selectedOptions + ")"),
                    quote.options(), font, language);
            addPriceRow(priceTable, t.discount(), quote.remise().negate(), font, language);
            addPriceRow(priceTable, t.deposit(), quote.caution(), font, language);
            addPriceRow(priceTable, t.vat(), quote.tva(), font, language);
            addPriceRow(priceTable, t.total(), quote.total(), font, language);
            document.add(priceTable);

            addSection(document, t.terms(), font, language);
            addParagraph(document, t.provisionalTermsNotice(), font, 9,
                    new DeviceRgb(160, 70, 0), true, language);
            for (String term : t.provisionalTerms()) {
                document.add(new Paragraph(term).setFont(font).setFontSize(9)
                        .setBaseDirection(baseDirection(language)));
            }

            addSection(document, t.signatures(), font, language);
            document.add(new Paragraph("\n\n" + t.clientSignature() + "                         "
                    + t.agencySignature() + "\n\n\n")
                    .setFont(font).setBaseDirection(baseDirection(language)));

            byte[] qrCode = createQrCode(digitalUrl);
            Image qrImage = new Image(ImageDataFactory.create(qrCode))
                    .scaleToFit(82, 82)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER);
            document.add(qrImage);
            document.add(new Paragraph(t.digitalVersion()).setFont(font).setFontSize(8)
                    .setTextAlignment(TextAlignment.CENTER));
            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            log.error("Impossible de générer le contrat PDF pour {}", reservation.getCode(), exception);
            throw new IllegalStateException("Impossible de générer le contrat PDF", exception);
        }
    }

    private void drawBrandHeader(PdfDocument pdf, PdfFont font) {
        PdfCanvas canvas = new PdfCanvas(pdf.addNewPage());
        canvas.saveState()
                .setFillColor(BRAND_BLUE)
                .circle(44, 790, 12)
                .fill()
                .restoreState();
        canvas.saveState()
                .setStrokeColor(ColorConstants.WHITE)
                .setLineWidth(1.7f)
                .moveTo(36, 790)
                .lineTo(52, 790)
                .moveTo(39, 786)
                .lineTo(49, 786)
                .stroke()
                .restoreState();
        canvas.beginText()
                .setFontAndSize(font, 19)
                .setColor(BRAND_BLUE, true)
                .moveText(64, 783)
                .showText("LocaVite")
                .endText();
    }

    private void addSection(Document document, String title, PdfFont font, ContractLanguage language) {
        document.add(new Paragraph(title).setFont(font).setFontSize(12).setBold()
                .setFontColor(BRAND_BLUE).setMarginTop(8).setMarginBottom(3)
                .setBaseDirection(baseDirection(language)));
        document.add(new LineSeparator(new SolidLine(0.7f)).setFontColor(BRAND_BLUE));
    }

    private void addParagraph(
            Document document,
            String text,
            PdfFont font,
            float size,
            com.itextpdf.kernel.colors.Color color,
            boolean bold,
            ContractLanguage language) {
        Paragraph paragraph = new Paragraph(shapeArabic(text, language)).setFont(font)
                .setFontSize(size).setFontColor(color).setBaseDirection(baseDirection(language));
        if (bold) {
            paragraph.setBold();
        }
        document.add(paragraph);
    }

    private void addPriceRow(
            Table table, String label, java.math.BigDecimal amount, PdfFont font, ContractLanguage language) {
        table.addCell(new Paragraph(shapeArabic(label, language)).setFont(font)
                .setBaseDirection(baseDirection(language)));
        table.addCell(new Paragraph(amount.toPlainString() + " MAD").setFont(font)
                .setTextAlignment(language == ContractLanguage.AR ? TextAlignment.LEFT : TextAlignment.RIGHT));
    }

    private String optionName(ReservationOption option, ContractLanguage language) {
        return switch (language) {
            case FR -> switch (option) {
                case GPS -> "GPS";
                case BABY_SEAT -> "Siège bébé";
                case EXTRA_DRIVER -> "Conducteur additionnel";
            };
            case EN -> switch (option) {
                case GPS -> "GPS";
                case BABY_SEAT -> "Baby seat";
                case EXTRA_DRIVER -> "Additional driver";
            };
            case AR -> switch (option) {
                case GPS -> "نظام تحديد المواقع";
                case BABY_SEAT -> "مقعد طفل";
                case EXTRA_DRIVER -> "سائق إضافي";
            };
        };
    }

    private String documentStatus(
            List<ma.locavite.document.domain.Document> documents,
            DocumentType type,
            Translation translation) {
        boolean validated = documents.stream()
                .anyMatch(document -> document.getType() == type
                        && document.getStatus() == DocumentStatus.VALIDATED);
        boolean uploaded = documents.stream().anyMatch(document -> document.getType() == type);
        return validated ? translation.providedAndValidated()
                : uploaded ? translation.providedNotValidated() : translation.notProvided();
    }

    private byte[] createQrCode(String value) throws Exception {
        BitMatrix matrix = new QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, 240, 240);
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

    private String createAccessToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String shapeArabic(String value, ContractLanguage language) {
        if (language != ContractLanguage.AR) {
            return value;
        }
        try {
            return new ArabicShaping(ArabicShaping.LETTERS_SHAPE).shape(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Impossible de préparer le texte arabe du contrat", exception);
        }
    }

    private BaseDirection baseDirection(ContractLanguage language) {
        return language == ContractLanguage.AR ? BaseDirection.RIGHT_TO_LEFT : BaseDirection.LEFT_TO_RIGHT;
    }

    private String localeTag(ContractLanguage language) {
        return switch (language) {
            case FR -> "fr-MA";
            case AR -> "ar-MA";
            case EN -> "en";
        };
    }

    private record Translation(
            String title,
            String contractNumber,
            String client,
            String identityDocuments,
            String drivingPermit,
            String vehicle,
            String brand,
            String model,
            String plate,
            String periodAndLocations,
            String startDate,
            String pickup,
            String endDate,
            String returnLocation,
            String priceDetails,
            String rental,
            String options,
            String discount,
            String deposit,
            String vat,
            String total,
            String terms,
            String provisionalTermsNotice,
            List<String> provisionalTerms,
            String signatures,
            String clientSignature,
            String agencySignature,
            String digitalVersion,
            String providedAndValidated,
            String providedNotValidated,
            String notProvided) {

        private static Translation forLanguage(ContractLanguage language) {
            return switch (language) {
                case FR -> new Translation(
                        "CONTRAT DE LOCATION DE VÉHICULE", "Numéro de contrat", "Client",
                        "CIN (pièce justificative)", "Permis de conduire (pièce justificative)",
                        "Véhicule", "Marque", "Modèle", "Immatriculation",
                        "Période et lieux", "Prise en charge", "Lieu", "Retour", "Lieu",
                        "Détail du prix", "Location", "Options", "Remise", "Caution", "TVA", "Total à payer",
                        "Conditions générales", "MODÈLE PROVISOIRE — CONDITIONS À VALIDER JURIDIQUEMENT",
                        List.of(
                                "Le véhicule est remis et restitué aux dates et lieux indiqués dans le présent contrat.",
                                "Le client s'engage à utiliser et restituer le véhicule dans l'état convenu, sous réserve de l'état des lieux contradictoire.",
                                "Le montant de la caution est une garantie et son éventuelle restitution est soumise à la vérification du véhicule.",
                                "Les modalités d'assurance, de carburant, de retard, d'annulation et de responsabilité doivent être complétées et validées par LocaVite avant toute utilisation réelle."
                        ),
                        "Signatures", "Signature du client", "Signature de l'agence",
                        "Version numérique — scanner le QR code",
                        "Pièce fournie et validée", "Pièce fournie, en attente de validation", "Pièce non fournie");
                case EN -> new Translation(
                        "VEHICLE RENTAL AGREEMENT", "Contract number", "Customer", "CIN (identity document)",
                        "Driving licence (supporting document)", "Vehicle", "Make", "Model", "Registration",
                        "Rental period and locations", "Pickup", "Location", "Return", "Location",
                        "Price details", "Rental", "Options", "Discount", "Security deposit", "VAT", "Total payable",
                        "Terms and conditions", "PROVISIONAL TEMPLATE — TERMS MUST BE LEGALLY APPROVED",
                        List.of(
                                "The vehicle is handed over and returned on the dates and at the locations specified in this agreement.",
                                "The customer agrees to use and return the vehicle in the agreed condition, subject to a joint inspection.",
                                "The security deposit is a guarantee; its possible refund is subject to a vehicle inspection.",
                                "Insurance, fuel, late return, cancellation and liability terms must be completed and approved by LocaVite before actual use."
                        ),
                        "Signatures", "Customer signature", "Agency signature",
                        "Digital version — scan the QR code",
                        "Document provided and validated", "Document provided, validation pending", "Document not provided");
                case AR -> new Translation(
                        "عقد كراء سيارة", "رقم العقد", "الزبون", "البطاقة الوطنية",
                        "رخصة السياقة", "السيارة", "العلامة", "الطراز", "رقم التسجيل",
                        "مدة ومكان الكراء", "تاريخ الاستلام", "المكان", "تاريخ الإرجاع", "المكان",
                        "تفاصيل السعر", "الكراء", "الإضافات", "التخفيض", "الضمان", "الضريبة على القيمة المضافة",
                        "المجموع المستحق", "الشروط العامة", "نموذج مؤقت — يجب اعتماد الشروط قانونياً",
                        List.of(
                                "يتم تسليم السيارة وإرجاعها في التواريخ والأماكن المحددة في هذا العقد.",
                                "يلتزم الزبون باستعمال السيارة وإرجاعها بالحالة المتفق عليها، مع إجراء معاينة مشتركة.",
                                "يشكل مبلغ الضمان كفالة، ويخضع إرجاعه المحتمل لمعاينة السيارة.",
                                "يجب استكمال واعتماد شروط التأمين والوقود والتأخير والإلغاء والمسؤولية من طرف LocaVite قبل الاستعمال الفعلي."
                        ),
                        "التوقيعات", "توقيع الزبون", "توقيع الوكالة",
                        "النسخة الرقمية — امسح رمز QR",
                        "الوثيقة مقدمة وتم التحقق منها", "الوثيقة مقدمة وفي انتظار التحقق", "الوثيقة غير مقدمة");
            };
        }
    }
}
