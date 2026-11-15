package ma.locavite.payment.service;

import ma.locavite.notification.domain.NotificationType;
import ma.locavite.notification.service.NotificationService;
import ma.locavite.payment.event.PaymentSucceededEvent;
import ma.locavite.reservation.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class PaymentSuccessSmsListener {
    private static final Logger log = LoggerFactory.getLogger(PaymentSuccessSmsListener.class);
    private final NotificationService notificationService;
    private final ReservationRepository reservationRepository;

    public PaymentSuccessSmsListener(NotificationService notificationService, ReservationRepository reservationRepository) {
        this.notificationService = notificationService;
        this.reservationRepository = reservationRepository;
    }

    @Async
    @EventListener
    public void onPaymentSucceeded(PaymentSucceededEvent event) {
        if (event == null || event.payment() == null || event.payment().getReservationId() == null) return;
        reservationRepository.findById(event.payment().getReservationId()).ifPresent(reservation -> {
            try {
                notificationService.sendNotification(reservation, NotificationType.PAYMENT_SUCCESS);
            } catch (Exception exception) {
                log.warn("Notification SMS de paiement impossible [paymentId={}]: {}",
                        event.payment().getId(), exception.getMessage());
            }
        });
    }
}
