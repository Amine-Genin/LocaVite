package ma.locavite.reservation.service;

import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.notification.fixtures.NotificationTestFixtures;
import ma.locavite.notification.service.NotificationService;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationStatus;
import ma.locavite.reservation.domain.Season;
import ma.locavite.reservation.dto.PriceQuoteResponse;
import ma.locavite.reservation.event.SeasonChangedEvent;
import ma.locavite.reservation.repository.ReservationRepository;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeasonChangedListenerTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private PricingService pricingService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    private SeasonChangedListener listener;

    private User client;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        listener = new SeasonChangedListener(
                reservationRepository,
                vehicleRepository,
                pricingService,
                notificationService,
                userRepository
        );

        client = NotificationTestFixtures.createClient(1L, "+212611223344", ma.locavite.notification.domain.NotificationPreference.SMS, true);
        vehicle = NotificationTestFixtures.createVehicle();
        vehicle.setId(50L);
        vehicle.setAgenceId(10L);
    }

    @Test
    @DisplayName("Recalcul automatique : met à jour le prix des réservations futures et notifie le client")
    void testRecalculateFutureReservationsAndNotifyClient() {
        LocalDate futureStart = LocalDate.now().plusDays(10);
        LocalDate futureEnd = LocalDate.now().plusDays(13);

        Reservation futureReservation = NotificationTestFixtures.createReservation(
                100L, "LV-FUTURE-01", client, vehicle, futureStart, futureEnd, ReservationStatus.CONFIRMED);
        futureReservation.setPrixTotal(new BigDecimal("1200.00")); // Ancien prix

        when(reservationRepository.findAll()).thenReturn(List.of(futureReservation));
        when(vehicleRepository.findById(50L)).thenReturn(Optional.of(vehicle));

        // Nouveau devis recalculé avec la nouvelle saison (ex: 1800.00)
        PriceQuoteResponse newQuote = new PriceQuoteResponse(
                new BigDecimal("1800.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("3000.00"),
                BigDecimal.ZERO,
                new BigDecimal("4800.00"), // Nouveau total avec caution
                3L
        );
        when(pricingService.calculate(eq(vehicle), eq(futureStart), eq(futureEnd), any())).thenReturn(newQuote);
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));

        Season highSeason = new Season("Haute saison", futureStart, futureEnd, new BigDecimal("1.50"), 3);

        int updated = listener.recalculateFutureReservations(highSeason);

        assertThat(updated).isEqualTo(1);
        assertThat(futureReservation.getPrixTotal()).isEqualByComparingTo("4800.00");
        verify(reservationRepository).save(futureReservation);
        verify(notificationService).sendNotification(eq(futureReservation), eq(ma.locavite.notification.domain.NotificationType.CONFIRMATION));
    }

    @Test
    @DisplayName("Recalcul : ignore les réservations passées ou annulées")
    void testRecalculateIgnoresPastOrCancelledReservations() {
        LocalDate pastStart = LocalDate.now().minusDays(5);
        LocalDate pastEnd = LocalDate.now().minusDays(2);

        Reservation pastReservation = NotificationTestFixtures.createReservation(
                101L, "LV-PAST-01", client, vehicle, pastStart, pastEnd, ReservationStatus.CONFIRMED);

        Reservation cancelledReservation = NotificationTestFixtures.createReservation(
                102L, "LV-CANCEL-01", client, vehicle, LocalDate.now().plusDays(5), LocalDate.now().plusDays(8), ReservationStatus.CANCELLED);

        when(reservationRepository.findAll()).thenReturn(List.of(pastReservation, cancelledReservation));

        Season season = new Season("Saison", LocalDate.now(), LocalDate.now().plusMonths(1), new BigDecimal("1.50"));

        int updated = listener.recalculateFutureReservations(season);

        assertThat(updated).isEqualTo(0);
        verifyNoInteractions(pricingService);
        verify(reservationRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }
}
