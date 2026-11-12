package ma.locavite.notification.service;

import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.notification.domain.NotificationLog;
import ma.locavite.notification.domain.NotificationPreference;
import ma.locavite.notification.domain.NotificationType;
import ma.locavite.notification.repository.NotificationLogRepository;
import ma.locavite.reservation.domain.ContractLanguage;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationContract;
import ma.locavite.reservation.repository.ReservationContractRepository;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final SmsService smsService;
    private final WhatsAppService whatsAppService;
    private final NotificationTemplateService templateService;
    private final NotificationLogRepository logRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final ReservationContractRepository contractRepository;

    public NotificationService(
            SmsService smsService,
            WhatsAppService whatsAppService,
            NotificationTemplateService templateService,
            NotificationLogRepository logRepository,
            UserRepository userRepository,
            VehicleRepository vehicleRepository,
            ReservationContractRepository contractRepository) {
        this.smsService = smsService;
        this.whatsAppService = whatsAppService;
        this.templateService = templateService;
        this.logRepository = logRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.contractRepository = contractRepository;
    }

    public boolean sendConfirmation(Reservation reservation) {
        return sendNotification(reservation, NotificationType.CONFIRMATION);
    }

    public boolean sendReminderJ1(Reservation reservation) {
        return sendNotification(reservation, NotificationType.REMINDER_J1);
    }

    public boolean sendCompletion(Reservation reservation) {
        return sendNotification(reservation, NotificationType.COMPLETION);
    }

    public boolean sendCancellation(Reservation reservation) {
        return sendNotification(reservation, NotificationType.CANCELLATION);
    }

    public boolean sendNotification(Reservation reservation, NotificationType type) {
        if (reservation == null) {
            log.warn("Réservation nulle fournie pour la notification");
            return false;
        }

        User client = resolveClient(reservation);
        if (client == null || client.getTelephone() == null || client.getTelephone().isBlank()) {
            log.warn("Numéro de téléphone introuvable pour le client de la réservation {}", reservation.getId());
            return false;
        }

        // Vérification Opt-in / Opt-out
        if (!client.isNotificationOptedIn() || client.getPreferenceNotification() == NotificationPreference.NONE) {
            log.info("Client {} opt-out pour les notifications. Envoi annulé [reservationId={}, type={}]",
                    client.getId(), reservation.getId(), type);
            saveOptOutLog(reservation.getId(), client.getId(), client.getTelephone(), type);
            return false;
        }

        Vehicle vehicle = resolveVehicle(reservation);
        ContractLanguage language = resolveLanguage(reservation);

        String message = templateService.buildMessage(type, language, reservation, vehicle, client);
        NotificationPreference preference = client.getPreferenceNotification();

        if (preference == NotificationPreference.WHATSAPP) {
            return whatsAppService.sendWhatsApp(reservation.getId(), client.getId(), client.getTelephone(), message, type);
        } else {
            return smsService.sendSms(reservation.getId(), client.getId(), client.getTelephone(), message, type);
        }
    }

    private User resolveClient(Reservation reservation) {
        if (reservation.getUserId() == null || userRepository == null) {
            return null;
        }
        return userRepository.findById(reservation.getUserId()).orElse(null);
    }

    private Vehicle resolveVehicle(Reservation reservation) {
        if (reservation.getVehicleId() == null || vehicleRepository == null) {
            return null;
        }
        return vehicleRepository.findById(reservation.getVehicleId()).orElse(null);
    }

    private ContractLanguage resolveLanguage(Reservation reservation) {
        if (contractRepository == null || reservation.getId() == null) {
            return ContractLanguage.FR;
        }
        try {
            java.util.List<ReservationContract> contracts = contractRepository.findByReservationId(reservation.getId());
            if (contracts != null && !contracts.isEmpty() && contracts.get(0).getLanguage() != null) {
                return contracts.get(0).getLanguage();
            }
        } catch (Exception e) {
            log.debug("Impossible de récupérer la langue du contrat : {}", e.getMessage());
        }
        return ContractLanguage.FR;
    }

    private void saveOptOutLog(Long reservationId, Long userId, String recipient, NotificationType type) {
        if (logRepository == null) {
            return;
        }
        try {
            String formatted = templateService.formatMoroccanPhoneNumber(recipient);
            NotificationLog logEntry = new NotificationLog(
                    reservationId,
                    userId,
                    formatted,
                    "SMS",
                    type.name(),
                    "Notification ignorée (client opt-out)",
                    "OPTED_OUT",
                    null,
                    0
            );
            logRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement du log opt-out : {}", e.getMessage());
        }
    }
}
