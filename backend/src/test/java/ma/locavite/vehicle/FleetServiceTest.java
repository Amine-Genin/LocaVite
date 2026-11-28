package ma.locavite.vehicle;

import ma.locavite.catalog.domain.Brand;
import ma.locavite.catalog.domain.Category;
import ma.locavite.payment.domain.Payment;
import ma.locavite.payment.domain.PaymentStatus;
import ma.locavite.payment.repository.PaymentRepository;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationStatus;
import ma.locavite.reservation.repository.ReservationRepository;
import ma.locavite.vehicle.domain.*;
import ma.locavite.vehicle.dto.VehicleStatusRequest;
import ma.locavite.vehicle.repository.*;
import ma.locavite.vehicle.service.CurrentUserService;
import ma.locavite.vehicle.service.FleetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FleetServiceTest {
    // Covers agency scoping, paid revenue aggregation, and status audit persistence.
    @Mock VehicleRepository vehicles;
    @Mock VehicleStatusHistoryRepository statusHistory;
    @Mock VehicleMaintenanceRepository maintenance;
    @Mock VehicleIncidentRepository incidents;
    @Mock ReservationRepository reservations;
    @Mock PaymentRepository payments;
    @Mock ma.locavite.auth.repository.UserRepository users;
    @Mock CurrentUserService currentUser;
    @InjectMocks FleetService service;

    @Test
    void fleetIsScopedToAgencyAndIncludesRevenue() {
        Vehicle vehicle = vehicle(10L, 4L);
        Reservation booking = new Reservation(); booking.setId(7L); booking.setVehicleId(10L);
        booking.setUserId(3L); booking.setCode("RES-7"); booking.setDateDebut(LocalDate.now().minusDays(4));
        booking.setDateFin(LocalDate.now().minusDays(1)); booking.setStatut(ReservationStatus.COMPLETED);
        Payment payment = new Payment(); payment.setReservationId(7L); payment.setStatut(PaymentStatus.SUCCESS);
        payment.setMontant(new BigDecimal("1250.00")); payment.setPaidAt(Instant.now()); payment.setCreatedAt(Instant.now());
        when(currentUser.isAdmin()).thenReturn(false); when(currentUser.getCurrentUserId()).thenReturn(Optional.of(4L));
        when(vehicles.findAll()).thenReturn(List.of(vehicle, vehicle(11L, 99L)));
        when(reservations.findAll()).thenReturn(List.of(booking)); when(payments.findAll()).thenReturn(List.of(payment));

        var result = service.getFleet(null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(10L);
        assertThat(result.get(0).revenusMois()).isEqualByComparingTo("1250.00");
        assertThat(result.get(0).revenusAnnee()).isEqualByComparingTo("1250.00");
    }

    @Test
    void changingStatusPersistsStatusAndHistory() {
        Vehicle vehicle = vehicle(10L, 4L);
        when(vehicles.findById(10L)).thenReturn(Optional.of(vehicle));
        when(currentUser.isAdmin()).thenReturn(false); when(currentUser.getCurrentUserId()).thenReturn(Optional.of(4L));
        when(vehicles.findAll()).thenReturn(List.of(vehicle)); when(reservations.findAll()).thenReturn(List.of());
        when(payments.findAll()).thenReturn(List.of());

        service.changeStatus(10L, new VehicleStatusRequest(VehicleStatus.MAINTENANCE, "Révision"));

        assertThat(vehicle.getStatut()).isEqualTo(VehicleStatus.MAINTENANCE);
        ArgumentCaptor<VehicleStatusHistory> event = ArgumentCaptor.forClass(VehicleStatusHistory.class);
        verify(statusHistory).save(event.capture());
        assertThat(event.getValue().getAncienStatut()).isEqualTo(VehicleStatus.PUBLISHED);
        assertThat(event.getValue().getNouveauStatut()).isEqualTo(VehicleStatus.MAINTENANCE);
        assertThat(event.getValue().getRaison()).isEqualTo("Révision");
    }

    private Vehicle vehicle(Long id, Long agencyId) {
        Brand brand = new Brand(); brand.setName("Dacia");
        Category category = new Category(); category.setName("SUV");
        Vehicle vehicle = new Vehicle(); vehicle.setId(id); vehicle.setAgenceId(agencyId); vehicle.setBrand(brand);
        vehicle.setCategory(category); vehicle.setMatricule("FLEET-" + id); vehicle.setModele("Duster");
        vehicle.setAnnee(2023); vehicle.setVille("Casablanca"); vehicle.setStatut(VehicleStatus.PUBLISHED);
        vehicle.setKilometrage(12000); return vehicle;
    }
}
