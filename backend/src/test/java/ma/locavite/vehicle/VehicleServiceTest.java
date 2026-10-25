package ma.locavite.vehicle;

import ma.locavite.vehicle.domain.*;
import ma.locavite.vehicle.dto.VehicleRequest;
import ma.locavite.vehicle.dto.VehicleResponse;
import ma.locavite.vehicle.exception.MatriculeAlreadyExistsException;
import ma.locavite.vehicle.mapper.VehicleMapper;
import ma.locavite.vehicle.repository.VehicleRepository;
import ma.locavite.vehicle.service.CurrentUserService;
import ma.locavite.vehicle.service.VehicleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock VehicleRepository repository;
    @Mock VehicleMapper mapper;
    @Mock CurrentUserService currentUser;
    @InjectMocks VehicleService service;

    private VehicleRequest sampleRequest() {
        return new VehicleRequest("Toyota", "Corolla", 2022, CategoryType.BERLINE, "Casablanca",
                new BigDecimal("350"), new BigDecimal("180000"), FuelType.DIESEL,
                TransmissionType.MANUELLE, 5, "Très bon état", " 12345-a-1 ", null);
    }

    @Test
    void create_shouldSaveVehicle() {
        VehicleRequest request = sampleRequest();
        VehicleResponse response = new VehicleResponse(1L, "Toyota", "Corolla", 2022, CategoryType.BERLINE,
                "Casablanca", new BigDecimal("350"), new BigDecimal("180000"), FuelType.DIESEL,
                TransmissionType.MANUELLE, 5, "Très bon état", "12345-A-1", VehicleStatus.DRAFT, 7L,
                Instant.now(), Instant.now());

        when(currentUser.getCurrentUserId()).thenReturn(Optional.of(7L));
        when(repository.existsByMatriculeIgnoreCase("12345-A-1")).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(new Vehicle());
        when(repository.saveAndFlush(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(Vehicle.class))).thenReturn(response);

        VehicleResponse result = service.create(request);

        ArgumentCaptor<Vehicle> captor = ArgumentCaptor.forClass(Vehicle.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getAgenceId()).isEqualTo(7L);
        assertThat(captor.getValue().getStatut()).isEqualTo(VehicleStatus.DRAFT);
        assertThat(captor.getValue().getMatricule()).isEqualTo("12345-A-1");
        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void create_shouldThrowIfMatriculeExists() {
        when(currentUser.getCurrentUserId()).thenReturn(Optional.of(7L));
        when(repository.existsByMatriculeIgnoreCase("12345-A-1")).thenReturn(true);

        assertThatThrownBy(() -> service.create(sampleRequest()))
                .isInstanceOf(MatriculeAlreadyExistsException.class);

        verify(repository, never()).saveAndFlush(any());
    }
}