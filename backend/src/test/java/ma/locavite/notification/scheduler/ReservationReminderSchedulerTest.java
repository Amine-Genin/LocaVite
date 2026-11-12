package ma.locavite.notification.scheduler;

import ma.locavite.auth.domain.User;
import ma.locavite.notification.domain.NotificationPreference;
import ma.locavite.notification.domain.NotificationType;
import ma.locavite.notification.fixtures.NotificationTestFixtures;
import ma.locavite.notification.repository.NotificationLogRepository;
import ma.locavite.notification.service.NotificationService;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationStatus;
import ma.locavite.reservation.repository.ReservationRepository;
import ma.locavite.vehicle.domain.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationReminderSchedulerTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationLogRepository logRepository;

    private ReservationReminderScheduler scheduler;

    private User client;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        scheduler = new ReservationReminderScheduler(reservationRepository, notificationService, logRepository);
        client = NotificationTestFixtures.createClient(1L, "+212611223344", NotificationPreference.SMS, true);
        vehicle = NotificationTestFixtures.createVehicle();
    }

    @Test
    @DisplayName("Test scheduler : envoie un rappel J-1 pour chaque réservation confirmée débutant demain")
    void testSendDailyRemindersSendsToEligibleReservations() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        Reservation res1 = NotificationTestFixtures.createReservation(
                10L, "LV-REMIND-1", client, vehicle, tomorrow, tomorrow.plusDays(3), ReservationStatus.CONFIRMED);
        Reservation res2 = NotificationTestFixtures.createReservation(
                20L, "LV-REMIND-2", client, vehicle, tomorrow, tomorrow.plusDays(5), ReservationStatus.CONFIRMED);

        when(reservationRepository.findByStatutAndDateDebut(ReservationStatus.CONFIRMED, tomorrow))
                .thenReturn(List.of(res1, res2));
        when(logRepository.existsByReservationIdAndNotificationTypeAndStatus(anyLong(), eq(NotificationType.REMINDER_J1.name()), eq("SENT")))
                .thenReturn(false);

        int count = scheduler.sendDailyReminders();

        assertThat(count).isEqualTo(2);
        verify(notificationService).sendReminderJ1(res1);
        verify(notificationService).sendReminderJ1(res2);
    }

    @Test
    @DisplayName("Test scheduler : ignore les réservations pour lesquelles le rappel J-1 a déjà été envoyé")
    void testSendDailyRemindersSkipsAlreadySent() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        Reservation res1 = NotificationTestFixtures.createReservation(
                10L, "LV-REMIND-1", client, vehicle, tomorrow, tomorrow.plusDays(3), ReservationStatus.CONFIRMED);

        when(reservationRepository.findByStatutAndDateDebut(ReservationStatus.CONFIRMED, tomorrow))
                .thenReturn(List.of(res1));
        when(logRepository.existsByReservationIdAndNotificationTypeAndStatus(10L, NotificationType.REMINDER_J1.name(), "SENT"))
                .thenReturn(true);

        int count = scheduler.sendDailyReminders();

        assertThat(count).isEqualTo(0);
        verify(notificationService, never()).sendReminderJ1(any());
    }
}
