package ma.locavite.notification.scheduler;

import ma.locavite.notification.domain.NotificationType;
import ma.locavite.notification.repository.NotificationLogRepository;
import ma.locavite.notification.service.NotificationService;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationStatus;
import ma.locavite.reservation.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class ReservationReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationReminderScheduler.class);

    private final ReservationRepository reservationRepository;
    private final NotificationService notificationService;
    private final NotificationLogRepository logRepository;

    public ReservationReminderScheduler(
            ReservationRepository reservationRepository,
            NotificationService notificationService,
            NotificationLogRepository logRepository) {
        this.reservationRepository = reservationRepository;
        this.notificationService = notificationService;
        this.logRepository = logRepository;
    }

    @Scheduled(cron = "${locavite.notifications.reminder-cron:0 0 9 * * *}")
    public int sendDailyReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        return sendRemindersForDate(tomorrow);
    }

    public int sendRemindersForDate(LocalDate targetDate) {
        log.info("Lancement du traitement des rappels de réservation J-1 pour la date : {}", targetDate);
        List<Reservation> reservations = reservationRepository.findByStatutAndDateDebut(
                ReservationStatus.CONFIRMED, targetDate);

        if (reservations.isEmpty()) {
            log.info("Aucune réservation confirmée pour la date {}", targetDate);
            return 0;
        }

        int count = 0;
        for (Reservation reservation : reservations) {
            try {
                boolean alreadySent = logRepository.existsByReservationIdAndNotificationTypeAndStatus(
                        reservation.getId(), NotificationType.REMINDER_J1.name(), "SENT");

                if (!alreadySent) {
                    notificationService.sendReminderJ1(reservation);
                    count++;
                } else {
                    log.debug("Rappel J-1 déjà envoyé pour la réservation {}", reservation.getId());
                }
            } catch (Exception e) {
                log.error("Erreur lors de l'envoi du rappel J-1 pour la réservation {} : {}",
                        reservation.getId(), e.getMessage());
            }
        }

        log.info("Fin du traitement des rappels J-1 : {} notifications traitées", count);
        return count;
    }
}
