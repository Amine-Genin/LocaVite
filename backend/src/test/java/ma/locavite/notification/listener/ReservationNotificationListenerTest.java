package ma.locavite.notification.listener;

import ma.locavite.auth.domain.User;
import ma.locavite.notification.domain.NotificationPreference;
import ma.locavite.notification.fixtures.NotificationTestFixtures;
import ma.locavite.notification.service.NotificationService;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationStatus;
import ma.locavite.reservation.event.ReservationCancelledEvent;
import ma.locavite.reservation.event.ReservationCompletedEvent;
import ma.locavite.reservation.event.ReservationConfirmedEvent;
import ma.locavite.vehicle.domain.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationNotificationListenerTest {

    @Mock
    private NotificationService notificationService;

    private ReservationNotificationListener listener;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        listener = new ReservationNotificationListener(notificationService);
        User client = NotificationTestFixtures.createClient(1L, "+212611223344", NotificationPreference.SMS, true);
        Vehicle vehicle = NotificationTestFixtures.createVehicle();
        reservation = NotificationTestFixtures.createReservation(
                100L, "LV-LISTEN-1", client, vehicle, LocalDate.now().plusDays(2), LocalDate.now().plusDays(5), ReservationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Test listener : confirmation déclenche notificationService.sendConfirmation")
    void onReservationConfirmed_triggersSendConfirmation() {
        listener.onReservationConfirmed(new ReservationConfirmedEvent(reservation, null));
        verify(notificationService).sendConfirmation(reservation);
    }

    @Test
    @DisplayName("Test listener : annulation déclenche notificationService.sendCancellation")
    void onReservationCancelled_triggersSendCancellation() {
        listener.onReservationCancelled(new ReservationCancelledEvent(reservation, null, null));
        verify(notificationService).sendCancellation(reservation);
    }

    @Test
    @DisplayName("Test listener : fin de location (retour) déclenche notificationService.sendCompletion")
    void onReservationCompleted_triggersSendCompletion() {
        listener.onReservationCompleted(new ReservationCompletedEvent(reservation));
        verify(notificationService).sendCompletion(reservation);
    }
}
