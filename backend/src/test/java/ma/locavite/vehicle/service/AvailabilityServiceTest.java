package ma.locavite.vehicle.service;

import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.domain.VehicleStatus;
import ma.locavite.vehicle.dto.AvailabilityResponse;
import ma.locavite.vehicle.dto.Period;
import ma.locavite.vehicle.dto.UnavailablePeriod;
import ma.locavite.vehicle.event.ReservationCreatedEvent;
import ma.locavite.vehicle.exception.VehicleNotFoundException;
import ma.locavite.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private ReservationProvider reservationProvider;

    @InjectMocks
    private AvailabilityService availabilityService;

    private Vehicle sampleVehicle(Long id, VehicleStatus status) {
        Vehicle v = new Vehicle();
        v.setId(id);
        v.setStatut(status);
        return v;
    }

    @Test
    @DisplayName("Test : véhicule sans réservation → tout disponible")
    void getAvailability_whenNoReservation_returnsFullPeriodAvailable() {
        Long vehicleId = 1L;
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);

        when(vehicleRepository.findById(vehicleId))
                .thenReturn(Optional.of(sampleVehicle(vehicleId, VehicleStatus.PUBLISHED)));
        when(reservationProvider.getUnavailablePeriods(vehicleId, from, to))
                .thenReturn(List.of());

        AvailabilityResponse response = availabilityService.getAvailability(vehicleId, from, to);

        assertThat(response).isNotNull();
        assertThat(response.unavailable()).isEmpty();
        assertThat(response.available()).hasSize(1);
        assertThat(response.available().get(0)).isEqualTo(new Period(from, to));

        verify(vehicleRepository).findById(vehicleId);
        verify(reservationProvider).getUnavailablePeriods(vehicleId, from, to);
    }

    @Test
    @DisplayName("Test : véhicule avec 1 réservation → période exclue et disponibles découpées")
    void getAvailability_whenOneReservation_excludesPeriodAndSplitsAvailable() {
        Long vehicleId = 1L;
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);

        LocalDate resStart = LocalDate.of(2026, 6, 10);
        LocalDate resEnd = LocalDate.of(2026, 6, 20);

        when(vehicleRepository.findById(vehicleId))
                .thenReturn(Optional.of(sampleVehicle(vehicleId, VehicleStatus.PUBLISHED)));
        when(reservationProvider.getUnavailablePeriods(vehicleId, from, to))
                .thenReturn(List.of(new UnavailablePeriod(resStart, resEnd, "RESERVATION")));

        AvailabilityResponse response = availabilityService.getAvailability(vehicleId, from, to);

        assertThat(response).isNotNull();
        assertThat(response.unavailable()).containsExactly(
                new UnavailablePeriod(resStart, resEnd, "RESERVATION")
        );
        assertThat(response.available()).containsExactly(
                new Period(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 9)),
                new Period(LocalDate.of(2026, 6, 21), LocalDate.of(2026, 12, 31))
        );
    }

    @Test
    @DisplayName("Test : réservation débutant le premier jour de la recherche")
    void getAvailability_whenReservationStartsAtBeginning() {
        Long vehicleId = 1L;
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);

        when(vehicleRepository.findById(vehicleId))
                .thenReturn(Optional.of(sampleVehicle(vehicleId, VehicleStatus.PUBLISHED)));
        when(reservationProvider.getUnavailablePeriods(vehicleId, from, to))
                .thenReturn(List.of(new UnavailablePeriod(
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 10),
                        "MAINTENANCE"
                )));

        AvailabilityResponse response = availabilityService.getAvailability(vehicleId, from, to);

        assertThat(response.unavailable()).hasSize(1);
        assertThat(response.available()).containsExactly(
                new Period(LocalDate.of(2026, 1, 11), LocalDate.of(2026, 1, 31))
        );
    }

    @Test
    @DisplayName("Test : réservation couvrant toute la période demandée")
    void getAvailability_whenReservationCoversWholeRange() {
        Long vehicleId = 1L;
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);

        when(vehicleRepository.findById(vehicleId))
                .thenReturn(Optional.of(sampleVehicle(vehicleId, VehicleStatus.PUBLISHED)));
        when(reservationProvider.getUnavailablePeriods(vehicleId, from, to))
                .thenReturn(List.of(new UnavailablePeriod(from, to, "RESERVATION")));

        AvailabilityResponse response = availabilityService.getAvailability(vehicleId, from, to);

        assertThat(response.available()).isEmpty();
        assertThat(response.unavailable()).containsExactly(
                new UnavailablePeriod(from, to, "RESERVATION")
        );
    }

    @Test
    @DisplayName("Test : véhicule introuvable → 404 VehicleNotFoundException")
    void getAvailability_whenVehicleNotFound_throwsException() {
        Long vehicleId = 999L;
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> availabilityService.getAvailability(vehicleId, from, to))
                .isInstanceOf(VehicleNotFoundException.class);

        verifyNoInteractions(reservationProvider);
    }

    @Test
    @DisplayName("Test : véhicule archivé → 404 VehicleNotFoundException")
    void getAvailability_whenVehicleArchived_throwsException() {
        Long vehicleId = 1L;
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);

        when(vehicleRepository.findById(vehicleId))
                .thenReturn(Optional.of(sampleVehicle(vehicleId, VehicleStatus.ARCHIVED)));

        assertThatThrownBy(() -> availabilityService.getAvailability(vehicleId, from, to))
                .isInstanceOf(VehicleNotFoundException.class);

        verifyNoInteractions(reservationProvider);
    }

    @Test
    @DisplayName("Test : dates invalides (to < from) → IllegalArgumentException")
    void getAvailability_whenEndBeforeStart_throwsException() {
        Long vehicleId = 1L;
        LocalDate from = LocalDate.of(2026, 12, 31);
        LocalDate to = LocalDate.of(2026, 1, 1);

        assertThatThrownBy(() -> availabilityService.getAvailability(vehicleId, from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La date de fin 'to' doit être postérieure ou égale à la date de début 'from'");

        verifyNoInteractions(vehicleRepository, reservationProvider);
    }

    @Test
    @DisplayName("Test : gestion de l'événement ReservationCreatedEvent")
    void onReservationCreated_handlesEventCleanly() {
        ReservationCreatedEvent event = new ReservationCreatedEvent(
                100L, 1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10)
        );

        availabilityService.onReservationCreated(event);
        // Méthode annotée avec @CacheEvict pour vider le cache
    }
}

