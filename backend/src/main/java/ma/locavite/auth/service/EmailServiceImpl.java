package ma.locavite.auth.service;

import jakarta.mail.internet.MimeMessage;
import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.document.service.MinioService;
import ma.locavite.mail.domain.EmailLog;
import ma.locavite.mail.repository.EmailLogRepository;
import ma.locavite.reservation.config.ContractProperties;
import ma.locavite.reservation.domain.ContractLanguage;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationContract;
import ma.locavite.reservation.repository.ReservationContractRepository;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private static final byte[] FALLBACK_PDF = (
            "%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n"
            + "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n"
            + "3 0 obj<</Type/Page/MediaBox[0 0 595 842]/Parent 2 0 R/Resources<<>>>>endobj\n"
            + "xref\n0 4\n0000000000 65535 f\n0000000009 00000 n\n0000000052 00000 n\n0000000101 00000 n\n"
            + "trailer<</Size 4/Root 1 0 R>>\nstartxref\n178\n%%EOF\n"
    ).getBytes(StandardCharsets.UTF_8);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailLogRepository emailLogRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final ReservationContractRepository contractRepository;
    private final MinioService minioService;
    private final ContractProperties contractProperties;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this(mailSender, null, null, null, null, null, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public EmailServiceImpl(
            JavaMailSender mailSender,
            TemplateEngine templateEngine,
            EmailLogRepository emailLogRepository,
            UserRepository userRepository,
            VehicleRepository vehicleRepository,
            ReservationContractRepository contractRepository,
            MinioService minioService,
            ContractProperties contractProperties) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.emailLogRepository = emailLogRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.contractRepository = contractRepository;
        this.minioService = minioService;
        this.contractProperties = contractProperties;
    }

    @Override
    public void sendConfirmationEmail(User user) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@locavite.ma");
            message.setTo(user.getEmail());
            message.setSubject("Bienvenue sur LocaVite - Confirmation d'inscription");
            message.setText("Bonjour " + user.getPrenom() + " " + user.getNom() + ",\n\n"
                    + "Votre compte client a été créé avec succès sur LocaVite.\n"
                    + "Vous pouvez dès à présent vous connecter et profiter de nos services de location de véhicules.\n\n"
                    + "Cordialement,\n"
                    + "L'équipe LocaVite");
            mailSender.send(message);
            log.info("Email de confirmation envoyé à {}", user.getEmail());
        } catch (Exception e) {
            log.error("Échec de l'envoi de l'email de confirmation à {} : {}", user.getEmail(), e.getMessage());
        }
    }

    @Override
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void sendReservationConfirmation(Reservation reservation) {
        sendReservationConfirmation(reservation, null, null);
    }

    @Override
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void sendReservationConfirmation(Reservation reservation, ReservationContract contract) {
        sendReservationConfirmation(reservation, contract, null);
    }

    @Override
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void sendReservationConfirmation(Reservation reservation, ReservationContract contract, ContractLanguage language) {
        if (reservation == null) {
            log.warn("Réservation nulle fournie pour l'envoi de confirmation");
            return;
        }

        User client = resolveClient(reservation);
        Vehicle vehicle = resolveVehicle(reservation);
        ContractLanguage lang = resolveLanguage(contract, language);
        String subject = resolveSubject(reservation, lang);
        String templateName = resolveTemplateName(lang);

        byte[] pdfBytes = resolveContractPdf(reservation, contract, lang);

        String baseUrl = contractProperties != null && contractProperties.getPublicBaseUrl() != null
                ? contractProperties.getPublicBaseUrl().replaceAll("/+$", "")
                : "http://localhost:8081";
        String reservationUrl = baseUrl + "/reservations/" + reservation.getCode();

        String htmlContent = renderHtml(client, reservation, vehicle, contract, reservationUrl, lang, templateName);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("noreply@locavite.ma");
            helper.setTo(client != null && client.getEmail() != null ? client.getEmail() : "client@locavite.ma");
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            if (pdfBytes != null && pdfBytes.length > 0) {
                helper.addAttachment("contrat-" + reservation.getCode() + ".pdf",
                        new ByteArrayResource(pdfBytes), "application/pdf");
            }

            mailSender.send(message);

            int attempt = 1;
            if (RetrySynchronizationManager.getContext() != null) {
                attempt = RetrySynchronizationManager.getContext().getRetryCount() + 1;
            }

            saveEmailLog(reservation.getId(),
                    client != null ? client.getEmail() : "inconnu",
                    subject, templateName, "SENT", null, attempt);

            log.info("Email de confirmation de réservation {} envoyé avec succès à {} (tentative {})",
                    reservation.getCode(), client != null ? client.getEmail() : "inconnu", attempt);
        } catch (Exception ex) {
            int attempt = 1;
            if (RetrySynchronizationManager.getContext() != null) {
                attempt = RetrySynchronizationManager.getContext().getRetryCount() + 1;
            }
            log.warn("Tentative {} échouée pour l'envoi d'email de réservation {}: {}",
                    attempt, reservation.getCode(), ex.getMessage());
            throw new RuntimeException("Échec de l'envoi du courriel: " + ex.getMessage(), ex);
        }
    }

    @Recover
    public void recover(Exception e, Reservation reservation) {
        recover(e, reservation, null, null);
    }

    @Recover
    public void recover(Exception e, Reservation reservation, ReservationContract contract) {
        recover(e, reservation, contract, null);
    }

    @Recover
    public void recover(Exception e, Reservation reservation, ReservationContract contract, ContractLanguage language) {
        log.error("Échec définitif après 3 tentatives d'envoi de confirmation pour la réservation {}: {}",
                reservation != null ? reservation.getCode() : "null", e.getMessage());

        User client = reservation != null ? resolveClient(reservation) : null;
        ContractLanguage lang = resolveLanguage(contract, language);
        String subject = reservation != null ? resolveSubject(reservation, lang) : "Confirmation de réservation LocaVite";
        String templateName = resolveTemplateName(lang);

        saveEmailLog(
                reservation != null ? reservation.getId() : null,
                client != null && client.getEmail() != null ? client.getEmail() : "inconnu",
                subject,
                templateName,
                "FAILED",
                e.getMessage(),
                3
        );
    }

    private User resolveClient(Reservation reservation) {
        if (reservation.getUserId() != null && userRepository != null) {
            return userRepository.findById(reservation.getUserId()).orElse(null);
        }
        return null;
    }

    private Vehicle resolveVehicle(Reservation reservation) {
        if (reservation.getVehicleId() != null && vehicleRepository != null) {
            return vehicleRepository.findById(reservation.getVehicleId()).orElse(null);
        }
        return null;
    }

    private ContractLanguage resolveLanguage(ReservationContract contract, ContractLanguage preferred) {
        if (preferred != null) {
            return preferred;
        }
        if (contract != null && contract.getLanguage() != null) {
            return contract.getLanguage();
        }
        return ContractLanguage.FR;
    }

    private String resolveSubject(Reservation reservation, ContractLanguage language) {
        String code = reservation != null && reservation.getCode() != null ? reservation.getCode() : "";
        return switch (language) {
            case EN -> "Your LocaVite booking confirmation - " + code;
            case AR -> "تأكيد حجزكم لدى لوكافيت - " + code;
            case FR -> "Votre réservation LocaVite est confirmée - " + code;
        };
    }

    private String resolveTemplateName(ContractLanguage language) {
        return "mail/reservation-confirmation-" + language.name().toLowerCase(Locale.ROOT);
    }

    private byte[] resolveContractPdf(Reservation reservation, ReservationContract contract, ContractLanguage lang) {
        if (contract != null && contract.getObjectPath() != null && minioService != null) {
            try {
                return minioService.getContract(contract.getObjectPath());
            } catch (Exception e) {
                log.warn("Impossible de récupérer le contrat PDF MinIO [{}]: {}", contract.getObjectPath(), e.getMessage());
            }
        }

        if (contractRepository != null && reservation != null && reservation.getId() != null && minioService != null) {
            try {
                ReservationContract found = contractRepository
                        .findByReservationIdAndLanguage(reservation.getId(), lang)
                        .orElse(null);
                if (found != null && found.getObjectPath() != null) {
                    return minioService.getContract(found.getObjectPath());
                }
            } catch (Exception e) {
                log.warn("Impossible de charger le contrat en base pour la réservation {}: {}",
                        reservation.getCode(), e.getMessage());
            }
        }

        return FALLBACK_PDF;
    }

    private String renderHtml(User client, Reservation reservation, Vehicle vehicle,
                              ReservationContract contract, String reservationUrl,
                              ContractLanguage lang, String templateName) {
        if (templateEngine == null) {
            return "<html><body><h1>LocaVite</h1><p>Réservation " + reservation.getCode() + " confirmée.</p>"
                    + "<a href=\"" + reservationUrl + "\">Voir ma réservation</a></body></html>";
        }

        Context context = new Context();
        context.setVariable("client", client != null ? client : new User());
        context.setVariable("reservation", reservation);
        context.setVariable("vehicle", vehicle);
        context.setVariable("contract", contract);
        context.setVariable("reservationUrl", reservationUrl);
        context.setVariable("lang", lang.name());

        try {
            return templateEngine.process(templateName, context);
        } catch (Exception ex) {
            log.warn("Erreur lors du rendu du template {}, bascule sur mail/reservation-confirmation: {}",
                    templateName, ex.getMessage());
            return templateEngine.process("mail/reservation-confirmation", context);
        }
    }

    private void saveEmailLog(Long reservationId, String recipient, String subject,
                              String templateName, String status, String errorMessage, int attempts) {
        if (emailLogRepository != null) {
            try {
                EmailLog emailLog = new EmailLog(reservationId, recipient, subject, templateName, status, errorMessage, attempts);
                emailLogRepository.save(emailLog);
            } catch (Exception ex) {
                log.error("Erreur lors de la sauvegarde du log email en base: {}", ex.getMessage());
            }
        }
    }
}
