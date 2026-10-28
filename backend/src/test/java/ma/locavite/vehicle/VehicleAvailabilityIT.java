package ma.locavite.vehicle;

import ma.locavite.catalog.domain.Brand;
import ma.locavite.catalog.domain.Category;
import ma.locavite.catalog.repository.BrandRepository;
import ma.locavite.catalog.repository.CategoryRepository;
import ma.locavite.vehicle.domain.FuelType;
import ma.locavite.vehicle.domain.TransmissionType;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.domain.VehicleStatus;
import ma.locavite.vehicle.dto.UnavailablePeriod;
import ma.locavite.vehicle.event.ReservationCreatedEvent;
import ma.locavite.vehicle.repository.VehicleRepository;
import ma.locavite.vehicle.service.ReservationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.cache.type=simple")
@AutoConfigureMockMvc
class VehicleAvailabilityIT {

    @Autowired
    private MockMvc mockMvc;

    @SpyBean
    private VehicleRepository vehicleRepository;

    @MockBean
    private ReservationProvider reservationProvider;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        if (cacheManager.getCache("vehicleAvailability") != null) {
            cacheManager.getCache("vehicleAvailability").clear();
        }
        vehicleRepository.deleteAll();

        Brand brand = brandRepository.findByNameIgnoreCase("Renault").orElseGet(() -> {
            Brand b = new Brand();
            b.setName("Renault");
            return brandRepository.save(b);
        });

        Category category = categoryRepository.findByNameIgnoreCase("BERLINE").orElseGet(() -> {
            Category c = new Category();
            c.setName("BERLINE");
            return categoryRepository.save(c);
        });

        vehicle = new Vehicle();
        vehicle.setBrand(brand);
        vehicle.setCategory(category);
        vehicle.setModele("Megane");
        vehicle.setAnnee(2022);
        vehicle.setVille("Rabat");
        vehicle.setPrixJour(new BigDecimal("320.00"));
        vehicle.setPrixVente(new BigDecimal("160000.00"));
        vehicle.setCarburant(FuelType.DIESEL);
        vehicle.setTransmission(TransmissionType.MANUELLE);
        vehicle.setPlaces(5);
        vehicle.setDescription("Berline confortable");
        vehicle.setMatricule("55555-A-1");
        vehicle.setStatut(VehicleStatus.PUBLISHED);
        vehicle.setAgenceId(1L);

        vehicle = vehicleRepository.saveAndFlush(vehicle);
        reset(vehicleRepository);
    }

    @Test
    @DisplayName("Test : endpoint GET /api/vehicles/{id}/availability sans réservation → tout disponible")
    void getAvailability_whenNoReservation_returnsFullPeriod() throws Exception {
        when(reservationProvider.getUnavailablePeriods(any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/vehicles/" + vehicle.getId() + "/availability")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available", hasSize(1)))
                .andExpect(jsonPath("$.available[0].from").value("2026-01-01"))
                .andExpect(jsonPath("$.available[0].to").value("2026-12-31"))
                .andExpect(jsonPath("$.unavailable", hasSize(0)));
    }

    @Test
    @DisplayName("Test : endpoint avec 1 réservation → période exclue et disponibles découpées")
    void getAvailability_whenOneReservation_excludesPeriod() throws Exception {
        LocalDate resStart = LocalDate.of(2026, 4, 10);
        LocalDate resEnd = LocalDate.of(2026, 4, 15);

        when(reservationProvider.getUnavailablePeriods(eq(vehicle.getId()), any(), any()))
                .thenReturn(List.of(new UnavailablePeriod(resStart, resEnd, "RESERVATION")));

        mockMvc.perform(get("/api/vehicles/" + vehicle.getId() + "/availability")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available", hasSize(2)))
                .andExpect(jsonPath("$.available[0].from").value("2026-01-01"))
                .andExpect(jsonPath("$.available[0].to").value("2026-04-09"))
                .andExpect(jsonPath("$.available[1].from").value("2026-04-16"))
                .andExpect(jsonPath("$.available[1].to").value("2026-12-31"))
                .andExpect(jsonPath("$.unavailable", hasSize(1)))
                .andExpect(jsonPath("$.unavailable[0].from").value("2026-04-10"))
                .andExpect(jsonPath("$.unavailable[0].to").value("2026-04-15"))
                .andExpect(jsonPath("$.unavailable[0].reason").value("RESERVATION"));
    }

    @Test
    @DisplayName("Test : cache court et invalidation immédiate après nouvelle réservation via Event")
    void getAvailability_cachingAndEventInvalidation() throws Exception {
        when(reservationProvider.getUnavailablePeriods(any(), any(), any()))
                .thenReturn(List.of());

        // 1er appel : interroge la DB
        mockMvc.perform(get("/api/vehicles/" + vehicle.getId() + "/availability")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isOk());
        verify(vehicleRepository, times(1)).findById(vehicle.getId());

        // 2ème appel avec mêmes dates : servi par le cache sans nouvelle requête DB
        mockMvc.perform(get("/api/vehicles/" + vehicle.getId() + "/availability")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isOk());
        verify(vehicleRepository, times(1)).findById(vehicle.getId());

        // Publication de l'événement ReservationCreatedEvent
        eventPublisher.publishEvent(new ReservationCreatedEvent(
                1L, vehicle.getId(), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 5)
        ));

        // 3ème appel après événement : le cache a été invalidé, réinterroge la DB
        mockMvc.perform(get("/api/vehicles/" + vehicle.getId() + "/availability")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isOk());
        verify(vehicleRepository, times(2)).findById(vehicle.getId());
    }

    @Test
    @DisplayName("Test : véhicule inexistant → 404")
    void getAvailability_unknownVehicle_returns404() throws Exception {
        mockMvc.perform(get("/api/vehicles/999999/availability")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Test : dates manquantes → 400 Bad Request")
    void getAvailability_missingDates_returns400() throws Exception {
        mockMvc.perform(get("/api/vehicles/" + vehicle.getId() + "/availability"))
                .andExpect(status().isBadRequest());
    }
}

