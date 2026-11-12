package ma.locavite.notification.listener;

import ma.locavite.notification.service.NotificationService;
import ma.locavite.reservation.event.ReservationCancelledEvent;
import ma.locavite.reservation.event.ReservationCompletedEvent;
import ma.locavite.reservation.event.ReservationConfirmedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class ReservationNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(ReservationNotificationListener.class);

    private final NotificationService notificationService;

    public ReservationNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Async
    @EventListener
    public void onReservationConfirmed(ReservationConfirmedEvent event) {
        if (event == null || event.reservation() == null) {
            return;
        }
        try {
            notificationService.sendConfirmation(event.reservation());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification de confirmation SMS/WhatsApp [reservationId={}] : {}",
                    event.reservation().getId(), e.getMessage());
        }
    }

    @Async
    @EventListener
    public void onReservationCancelled(ReservationCancelledEvent event) {
        if (event == null || event.reservation() == null) {
            return;
        }
        try {
            notificationService.sendCancellation(event.reservation());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification d'annulation SMS/WhatsApp [reservationId={}] : {}",
                    event.reservation().getId(), e.getMessage());
        }
    }

    @Async
    @EventListener
    public void onReservationCompleted(ReservationCompletedEvent event) {
        if (event == null || event.reservation() == null) {
            return;
        }
        try {
            notificationService.sendCompletion(event.reservation());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification de fin de location SMS/WhatsApp [reservationId={}] : {}",
                    event.reservation().getId(), e.getMessage());
        }
    }
}
