package ma.locavite.payment.service;

import jakarta.mail.internet.MimeMessage;
import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.mail.domain.EmailLog;
import ma.locavite.mail.repository.EmailLogRepository;
import ma.locavite.notification.domain.NotificationPreference;
import ma.locavite.notification.domain.NotificationLog;
import ma.locavite.notification.repository.NotificationLogRepository;
import ma.locavite.payment.domain.Payment;
import ma.locavite.payment.domain.PaymentStatus;
import ma.locavite.payment.event.CashPaymentConfirmedEvent;
import ma.locavite.payment.event.PaymentFailedEvent;
import ma.locavite.payment.service.CashReceiptPdfService;
import ma.locavite.reservation.config.ContractProperties;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.repository.ReservationContractRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import ma.locavite.reservation.domain.ContractLanguage;
import java.util.Locale;


@Service
public class PaymentNotificationService {
    private static final Logger log = LoggerFactory.getLogger(PaymentNotificationService.class);
    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;
    private final UserRepository userRepository;
    private final TemplateEngine templateEngine;
    private final CashReceiptPdfService receiptPdfService;
    private final ReservationContractRepository contractRepository;
    private final ContractProperties contractProperties;
    private final NotificationLogRepository notificationLogRepository;
    @Value("${locavite.frontend.url:http://localhost:4200}")
    private String frontendUrl = "http://localhost:4200";

    public PaymentNotificationService(JavaMailSender mailSender, EmailLogRepository emailLogRepository,
            UserRepository userRepository, TemplateEngine templateEngine,
            CashReceiptPdfService receiptPdfService, ReservationContractRepository contractRepository,
            ContractProperties contractProperties, NotificationLogRepository notificationLogRepository) {
        this.mailSender = mailSender;
        this.emailLogRepository = emailLogRepository;
        this.userRepository = userRepository;
        this.templateEngine = templateEngine;
        this.receiptPdfService = receiptPdfService;
        this.contractRepository = contractRepository;
        this.contractProperties = contractProperties;
        this.notificationLogRepository = notificationLogRepository;
    }

    @Async
    @EventListener
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void onPaymentFailed(PaymentFailedEvent event) {
        if (event == null || event.payment() == null || event.payment().getStatut() != PaymentStatus.FAILED) return;
        Payment payment = event.payment();
        User user = userRepository.findById(payment.getUserId()).orElse(null);
        if (!eligible(user)) { logOptOut(user, payment, "PAYMENT_FAILED"); return; }
        String link = frontendUrl.replaceAll("/+$", "") + "/payment/failure?paymentId=" + payment.getId()
                + "&reservationId=" + payment.getReservationId();
        sendEmail(user, payment, "PAYMENT_FAILED", "Paiement échoué",
                "mail/payment-failed", null, link);
    }

    @Async
    @EventListener
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void onCashPaymentConfirmed(CashPaymentConfirmedEvent event) {
        if (event == null || event.payment() == null || event.reservation() == null) return;
        Payment payment = event.payment();
        User user = userRepository.findById(payment.getUserId()).orElse(null);
        if (!eligible(user)) { logOptOut(user, payment, "CASH_PAYMENT_CONFIRMED"); return; }
        String code = payment.getCode() != null ? payment.getCode() : String.valueOf(payment.getId());
        byte[] pdf = receiptPdfService.generate(payment, event.reservation());
        sendEmail(user, payment, "CASH_PAYMENT_CONFIRMED", "Confirmation de paiement",
                "mail/payment-cash-confirmed", pdf, code);
    }

    public void sendEmail(User user, Payment payment, String type, String mailSubject,
                          String template, byte[] attachment, String value) {
        int attempt = RetrySynchronizationManager.getContext() == null ? 1
                : RetrySynchronizationManager.getContext().getRetryCount() + 1;
        String recipient = user.getEmail();
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, attachment != null, "UTF-8");
            ContractLanguage language = resolveLanguage(payment);
            String localizedSubject = localizedSubject(type, language);
            helper.setFrom("noreply@locavite.ma"); helper.setTo(recipient); helper.setSubject(localizedSubject);
            Context context = new Context(Locale.forLanguageTag(language.name().toLowerCase(Locale.ROOT)));
            context.setVariable("payment", payment);
            context.setVariable("value", value);
            context.setVariable("retryLink", value);
            context.setVariable("isRtl", language == ContractLanguage.AR);
            helper.setText(templateEngine.process(template, context), true);
            if (attachment != null) helper.addAttachment("recu-" + value + ".pdf", new ByteArrayResource(attachment), "application/pdf");
            mailSender.send(message);
            emailLogRepository.save(new EmailLog(payment.getReservationId(), recipient, localizedSubject, template, "SENT", null, attempt));
        } catch (Exception ex) {
            emailLogRepository.save(new EmailLog(payment.getReservationId(), recipient, mailSubject, template, "FAILED", ex.getMessage(), attempt));
            throw new IllegalStateException("Échec d'envoi de notification de paiement", ex);
        }
    }

    @Recover
    public void recoverFailed(IllegalStateException error, PaymentFailedEvent event) {
        log.error("Notification email de paiement échouée après trois tentatives [paymentId={}]: {}",
                event.payment().getId(), error.getMessage());
    }

    @Recover
    public void recoverCash(IllegalStateException error, CashPaymentConfirmedEvent event) {
        log.error("Notification email de reçu échouée après trois tentatives [paymentId={}]: {}",
                event.payment().getId(), error.getMessage());
    }

    private boolean eligible(User user) {
        return user != null && user.isNotificationOptedIn()
                && user.getPreferenceNotification() != NotificationPreference.NONE
                && user.getEmail() != null && !user.getEmail().isBlank();
    }

    private void logOptOut(User user, Payment payment, String type) {
        if (user == null) return;
        String recipient = user.getEmail() != null ? user.getEmail() : user.getTelephone();
        if (recipient == null || recipient.isBlank()) return;
        notificationLogRepository.save(new NotificationLog(payment.getReservationId(), user.getId(),
                recipient, "EMAIL", type, "Notification ignorée (client opt-out)", "OPTED_OUT", null, 0));
    }

    private ContractLanguage resolveLanguage(Payment payment) {
        try {
            return contractRepository.findByReservationId(payment.getReservationId()).stream()
                    .findFirst().map(contract -> contract.getLanguage() != null ? contract.getLanguage() : ContractLanguage.FR)
                    .orElse(ContractLanguage.FR);
        } catch (Exception ignored) { return ContractLanguage.FR; }
    }

    private String localizedSubject(String type, ContractLanguage language) {
        if (type.equals("PAYMENT_FAILED")) return switch (language) {
            case EN -> "Payment failed - LocaVite"; case AR -> "فشل الدفع - LocaVite"; default -> "Paiement échoué - LocaVite";
        };
        return switch (language) {
            case EN -> "Payment receipt - LocaVite"; case AR -> "إيصال الدفع - LocaVite"; default -> "Reçu de paiement - LocaVite";
        };
    }
}
