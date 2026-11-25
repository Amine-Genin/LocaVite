package ma.locavite.sale.service;

import jakarta.mail.internet.MimeMessage;
import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.mail.domain.EmailLog;
import ma.locavite.mail.repository.EmailLogRepository;
import ma.locavite.notification.domain.NotificationChannel;
import ma.locavite.notification.domain.NotificationLog;
import ma.locavite.notification.domain.NotificationPreference;
import ma.locavite.notification.domain.NotificationStatus;
import ma.locavite.notification.domain.NotificationType;
import ma.locavite.notification.repository.NotificationLogRepository;
import ma.locavite.notification.service.SmsService;
import ma.locavite.reservation.domain.ContractLanguage;
import ma.locavite.sale.domain.ContactRequest;
import ma.locavite.sale.domain.SaleListing;
import ma.locavite.sale.domain.VisitRequest;
import ma.locavite.sale.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;

@Service
public class SaleNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SaleNotificationService.class);

    private final JavaMailSender mailSender;
    private final SmsService smsService;
    private final TemplateEngine templateEngine;
    private final UserRepository userRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final EmailLogRepository emailLogRepository;

    public SaleNotificationService(
            JavaMailSender mailSender,
            SmsService smsService,
            TemplateEngine templateEngine,
            UserRepository userRepository,
            NotificationLogRepository notificationLogRepository,
            EmailLogRepository emailLogRepository) {
        this.mailSender = mailSender;
        this.smsService = smsService;
        this.templateEngine = templateEngine;
        this.userRepository = userRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.emailLogRepository = emailLogRepository;
    }

    public void notifyNewContactRequest(ContactRequest request, SaleListing listing) {
        User agency = userRepository.findById(listing.getAgenceId()).orElse(null);
        if (agency == null) {
            log.warn("Agence introuvable pour la notification de contact [listingId={}]", listing.getId());
            return;
        }

        NotificationType type = NotificationType.SALE_CONTACT_REQUEST;
        if (!isOptedIn(agency, NotificationChannel.EMAIL)) {
            logOptOut(agency, type, NotificationChannel.EMAIL, listing.getId());
            return;
        }

        String template = "mail/sale/contact-request";
        ContractLanguage lang = resolveUserLanguage(agency);
        String subject = switch (lang) {
            case EN -> "New Contact Inquiry - LocaVite - " + listing.getTitre();
            case AR -> "طلب اتصال جديد - لوكافيت - " + listing.getTitre();
            default -> "Nouvelle demande de contact - LocaVite - " + listing.getTitre();
        };

        Context context = new Context(resolveLocale(lang));
        context.setVariable("listing", listing);
        context.setVariable("contactRequest", request);
        context.setVariable("isRtl", lang == ContractLanguage.AR);

        sendEmail(agency, type, subject, template, context, listing.getId());
    }

    public void notifyNewVisitRequest(VisitRequest visit, SaleListing listing) {
        User agency = userRepository.findById(listing.getAgenceId()).orElse(null);
        if (agency == null) {
            log.warn("Agence introuvable pour la demande de visite [listingId={}]", listing.getId());
            return;
        }

        NotificationType type = NotificationType.SALE_VISIT_REQUEST;

        // 1. Email agence
        if (isOptedIn(agency, NotificationChannel.EMAIL)) {
            String template = "mail/sale/visit-request";
            ContractLanguage lang = resolveUserLanguage(agency);
            String subject = switch (lang) {
                case EN -> "New Visit Request - LocaVite - " + listing.getTitre();
                case AR -> "طلب زيارة جديد - لوكافيت - " + listing.getTitre();
                default -> "Nouvelle demande de visite - LocaVite - " + listing.getTitre();
            };

            Context context = new Context(resolveLocale(lang));
            context.setVariable("listing", listing);
            context.setVariable("visitRequest", visit);
            context.setVariable("isRtl", lang == ContractLanguage.AR);

            sendEmail(agency, type, subject, template, context, listing.getId());
        } else {
            logOptOut(agency, type, NotificationChannel.EMAIL, listing.getId());
        }

        // 2. SMS agence
        if (isOptedIn(agency, NotificationChannel.SMS)) {
            ContractLanguage lang = resolveUserLanguage(agency);
            String smsBody = switch (lang) {
                case EN -> String.format("LocaVite: New %s requested for %s on %s at %s.",
                        visit.getTypeVisite(), listing.getTitre(), visit.getDateSouhaitee(), visit.getCreneauHoraire());
                case AR -> String.format("لوكافيت: طلب %s جديد للسيارة %s بتاريخ %s في %s.",
                        visit.getTypeVisite(), listing.getTitre(), visit.getDateSouhaitee(), visit.getCreneauHoraire());
                default -> String.format("LocaVite: Nouvelle demande de %s pour %s le %s à %s.",
                        visit.getTypeVisite(), listing.getTitre(), visit.getDateSouhaitee(), visit.getCreneauHoraire());
            };
            sendSms(agency, type, smsBody, listing.getId());
        } else {
            logOptOut(agency, type, NotificationChannel.SMS, listing.getId());
        }
    }

    public void notifyVisitConfirmed(VisitRequest visit, SaleListing listing) {
        User client = userRepository.findById(visit.getClientId()).orElse(null);
        if (client == null) {
            log.warn("Client introuvable pour la confirmation de visite [visitId={}]", visit.getId());
            return;
        }

        NotificationType type = NotificationType.SALE_VISIT_CONFIRMED;

        // 1. Email client
        if (isOptedIn(client, NotificationChannel.EMAIL)) {
            String template = "mail/sale/visit-confirmed";
            ContractLanguage lang = resolveUserLanguage(client);
            String subject = switch (lang) {
                case EN -> "Visit Confirmed - LocaVite - " + listing.getTitre();
                case AR -> "تأكيد موعد الزيارة - لوكافيت - " + listing.getTitre();
                default -> "Visite confirmée - LocaVite - " + listing.getTitre();
            };

            Context context = new Context(resolveLocale(lang));
            context.setVariable("listing", listing);
            context.setVariable("visitRequest", visit);
            context.setVariable("isRtl", lang == ContractLanguage.AR);

            sendEmail(client, type, subject, template, context, listing.getId());
        } else {
            logOptOut(client, type, NotificationChannel.EMAIL, listing.getId());
        }

        // 2. SMS client
        if (isOptedIn(client, NotificationChannel.SMS)) {
            ContractLanguage lang = resolveUserLanguage(client);
            String smsBody = switch (lang) {
                case EN -> String.format("LocaVite: Your visit for %s is confirmed on %s at %s (%s).",
                        listing.getTitre(), visit.getDateSouhaitee(), visit.getCreneauHoraire(), visit.getLieu());
                case AR -> String.format("لوكافيت: تم تأكيد موعد زيارتكم للسيارة %s في %s الساعة %s (%s).",
                        listing.getTitre(), visit.getDateSouhaitee(), visit.getCreneauHoraire(), visit.getLieu());
                default -> String.format("LocaVite: Votre visite pour %s est confirmée le %s à %s (%s).",
                        listing.getTitre(), visit.getDateSouhaitee(), visit.getCreneauHoraire(), visit.getLieu());
            };
            sendSms(client, type, smsBody, listing.getId());
        } else {
            logOptOut(client, type, NotificationChannel.SMS, listing.getId());
        }
    }

    public void notifyVisitReminderJ1(VisitRequest visit, SaleListing listing) {
        NotificationType type = NotificationType.SALE_VISIT_REMINDER_J1;
        User client = userRepository.findById(visit.getClientId()).orElse(null);
        User agency = userRepository.findById(listing.getAgenceId()).orElse(null);

        // SMS client
        if (client != null) {
            if (isOptedIn(client, NotificationChannel.SMS)) {
                ContractLanguage lang = resolveUserLanguage(client);
                String clientSms = switch (lang) {
                    case EN -> String.format("LocaVite Reminder: Your visit for %s is scheduled tomorrow %s at %s.",
                            listing.getTitre(), visit.getDateSouhaitee(), visit.getCreneauHoraire());
                    case AR -> String.format("لوكافيت: تذكير بموعد زيارتكم للسيارة %s غداً %s الساعة %s.",
                            listing.getTitre(), visit.getDateSouhaitee(), visit.getCreneauHoraire());
                    default -> String.format("LocaVite Rappel : Votre visite pour %s est prévue demain %s à %s.",
                            listing.getTitre(), visit.getDateSouhaitee(), visit.getCreneauHoraire());
                };
                sendSms(client, type, clientSms, listing.getId());
            } else {
                logOptOut(client, type, NotificationChannel.SMS, listing.getId());
            }
        }

        // SMS agence
        if (agency != null) {
            if (isOptedIn(agency, NotificationChannel.SMS)) {
                ContractLanguage lang = resolveUserLanguage(agency);
                String agencySms = switch (lang) {
                    case EN -> String.format("LocaVite Reminder: Visit scheduled tomorrow for %s with client at %s.",
                            listing.getTitre(), visit.getCreneauHoraire());
                    case AR -> String.format("لوكافيت: تذكير بموعد زيارة مقرر غداً للسيارة %s الساعة %s.",
                            listing.getTitre(), visit.getCreneauHoraire());
                    default -> String.format("LocaVite Rappel : Visite prévue demain pour %s à %s.",
                            listing.getTitre(), visit.getCreneauHoraire());
                };
                sendSms(agency, type, agencySms, listing.getId());
            } else {
                logOptOut(agency, type, NotificationChannel.SMS, listing.getId());
            }
        }
    }

    public void notifyListingPublished(SaleListing listing) {
        User agency = userRepository.findById(listing.getAgenceId()).orElse(null);
        if (agency == null) return;

        NotificationType type = NotificationType.SALE_LISTING_PUBLISHED;
        if (!isOptedIn(agency, NotificationChannel.EMAIL)) {
            logOptOut(agency, type, NotificationChannel.EMAIL, listing.getId());
            return;
        }

        String template = "mail/sale/listing-published";
        ContractLanguage lang = resolveUserLanguage(agency);
        String subject = switch (lang) {
            case EN -> "Listing Published - LocaVite - " + listing.getTitre();
            case AR -> "تم نشر الإعلان - لوكافيت - " + listing.getTitre();
            default -> "Annonce publiée - LocaVite - " + listing.getTitre();
        };

        Context context = new Context(resolveLocale(lang));
        context.setVariable("listing", listing);
        context.setVariable("isRtl", lang == ContractLanguage.AR);

        sendEmail(agency, type, subject, template, context, listing.getId());
    }

    public void notifyListingSold(SaleListing listing) {
        User agency = userRepository.findById(listing.getAgenceId()).orElse(null);
        if (agency == null) return;

        NotificationType type = NotificationType.SALE_LISTING_SOLD;
        if (!isOptedIn(agency, NotificationChannel.EMAIL)) {
            logOptOut(agency, type, NotificationChannel.EMAIL, listing.getId());
            return;
        }

        String template = "mail/sale/listing-sold";
        ContractLanguage lang = resolveUserLanguage(agency);
        String subject = switch (lang) {
            case EN -> "Listing Marked as Sold - LocaVite - " + listing.getTitre();
            case AR -> "تم تحديد الإعلان كـ مُباع - لوكافيت - " + listing.getTitre();
            default -> "Annonce vendue - LocaVite - " + listing.getTitre();
        };

        Context context = new Context(resolveLocale(lang));
        context.setVariable("listing", listing);
        context.setVariable("isRtl", lang == ContractLanguage.AR);

        sendEmail(agency, type, subject, template, context, listing.getId());
    }

    public void notifyMessageReceived(ContactRequest request, SaleListing listing, String replyMessage) {
        NotificationType type = NotificationType.SALE_MESSAGE_RECEIVED;
        String recipientEmail = request.getEmail();
        if (recipientEmail == null || recipientEmail.isBlank()) return;

        // Recherche si le demandeur correspond à un User enregistré pour vérifier son opt-in
        User recipientUser = userRepository.findByEmail(recipientEmail).orElse(null);
        if (recipientUser != null && !isOptedIn(recipientUser, NotificationChannel.EMAIL)) {
            logOptOut(recipientUser, type, NotificationChannel.EMAIL, listing.getId());
            return;
        }

        ContractLanguage lang = recipientUser != null ? resolveUserLanguage(recipientUser) : ContractLanguage.FR;
        String subject = switch (lang) {
            case EN -> "New message regarding your inquiry - LocaVite - " + listing.getTitre();
            case AR -> "رسالة جديدة بخصوص استفساركم - لوكافيت - " + listing.getTitre();
            default -> "Nouveau message concernant votre demande - LocaVite - " + listing.getTitre();
        };

        String template = "mail/sale/message-received";
        Context context = new Context(resolveLocale(lang));
        context.setVariable("listing", listing);
        context.setVariable("replyMessage", replyMessage);
        context.setVariable("isRtl", lang == ContractLanguage.AR);

        Long userId = recipientUser != null ? recipientUser.getId() : null;
        sendDirectEmail(recipientEmail, userId, type, subject, template, context, listing.getId());
    }

    // --- Helpers d'envoi et de logging ---

    private void sendEmail(User user, NotificationType type, String subject, String template, Context context, Long listingId) {
        sendDirectEmail(user.getEmail(), user.getId(), type, subject, template, context, listingId);
    }

    private void sendDirectEmail(String to, Long userId, NotificationType type, String subject, String template, Context context, Long listingId) {
        String body = "";
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom("noreply@locavite.ma");
            helper.setTo(to);
            helper.setSubject(subject);
            body = templateEngine.process(template, context);
            helper.setText(body, true);
            mailSender.send(message);

            notificationLogRepository.save(new NotificationLog(
                    listingId, userId, to, NotificationChannel.EMAIL.name(),
                    type.name(), body, NotificationStatus.SENT.name(), null, 1));
            emailLogRepository.save(new EmailLog(listingId, to, subject, template, "SENT", null, 1));
        } catch (Exception e) {
            log.error("Échec d'envoi de notification email vente [{}] à {} : {}", type, to, e.getMessage());
            notificationLogRepository.save(new NotificationLog(
                    listingId, userId, to, NotificationChannel.EMAIL.name(),
                    type.name(), body.isBlank() ? subject : body, NotificationStatus.FAILED.name(), e.getMessage(), 1));
            emailLogRepository.save(new EmailLog(listingId, to, subject, template, "FAILED", e.getMessage(), 1));
        }
    }

    private void sendSms(User user, NotificationType type, String message, Long listingId) {
        String phone = user.getTelephone();
        if (phone == null || phone.isBlank()) return;

        try {
            boolean sent = smsService.sendSms(phone, message);
            String status = sent ? NotificationStatus.SENT.name() : NotificationStatus.FAILED.name();
            String err = sent ? null : "SMS service returned false";
            notificationLogRepository.save(new NotificationLog(
                    listingId, user.getId(), phone, NotificationChannel.SMS.name(),
                    type.name(), message, status, err, 1));
        } catch (Exception e) {
            log.error("Échec d'envoi de SMS vente [{}] à {} : {}", type, phone, e.getMessage());
            notificationLogRepository.save(new NotificationLog(
                    listingId, user.getId(), phone, NotificationChannel.SMS.name(),
                    type.name(), message, NotificationStatus.FAILED.name(), e.getMessage(), 1));
        }
    }

    private boolean isOptedIn(User user, NotificationChannel channel) {
        if (user == null) return true;
        if (!user.isNotificationOptedIn()) return false;
        NotificationPreference pref = user.getPreferenceNotification();
        if (pref == NotificationPreference.NONE) return false;
        // Si l'utilisateur est opt-in et n'a pas spécifié de refus strict (NONE),
        // il reçoit les notifications email et SMS requises pour son canal.
        return true;
    }

    private void logOptOut(User user, NotificationType type, NotificationChannel channel, Long listingId) {
        String recipient = channel == NotificationChannel.EMAIL ? user.getEmail() : user.getTelephone();
        if (recipient == null || recipient.isBlank()) return;
        notificationLogRepository.save(new NotificationLog(
                listingId, user.getId(), recipient, channel.name(),
                type.name(), "Notification ignorée (utilisateur opt-out)",
                NotificationStatus.OPTED_OUT.name(), null, 0));
    }

    private ContractLanguage resolveUserLanguage(User user) {
        // Par défaut FR
        return ContractLanguage.FR;
    }

    private Locale resolveLocale(ContractLanguage lang) {
        return switch (lang) {
            case EN -> Locale.ENGLISH;
            case AR -> Locale.forLanguageTag("ar");
            default -> Locale.FRENCH;
        };
    }
}
