package ma.locavite.vehicle.service;

import ma.locavite.catalog.domain.Brand;
import ma.locavite.catalog.domain.Category;
import ma.locavite.vehicle.domain.FuelType;
import ma.locavite.vehicle.domain.TransmissionType;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.domain.VehicleStatus;
import ma.locavite.vehicle.dto.VehicleFilter;
import ma.locavite.vehicle.dto.VehicleResponse;
import ma.locavite.vehicle.mapper.VehicleMapper;
import ma.locavite.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleSearchServiceTest {

    @Mock
    private VehicleRepository repository;

    @Mock
    private VehicleMapper mapper;

    @InjectMocks
    private VehicleSearchService searchService;

    private Vehicle sampleVehicle() {
        Vehicle v = new Vehicle();
        v.setId(10L);
        v.setModele("Clio");
        v.setVille("Casablanca");
        v.setPrixJour(new BigDecimal("250.00"));
        v.setPrixVente(new BigDecimal("120000.00"));
        v.setCarburant(FuelType.DIESEL);
        v.setTransmission(TransmissionType.MANUELLE);
        v.setPlaces(5);
        v.setStatut(VehicleStatus.PUBLISHED);

        Brand brand = new Brand();
        brand.setId(1L);
        brand.setName("Renault");
        v.setBrand(brand);

        Category category = new Category();
        category.setId(1L);
        category.setName("CITADINE");
        v.setCategory(category);

        return v;
    }

    private VehicleResponse sampleResponse() {
        return new VehicleResponse(
                10L, 1L, "Renault", "Clio", 2022,
                1L, "CITADINE", "Casablanca",
                new BigDecimal("250.00"), new BigDecimal("120000.00"),
                FuelType.DIESEL, TransmissionType.MANUELLE,
                5, "Description", "12345-B-2",
                VehicleStatus.PUBLISHED, 2L,
                Instant.now(), Instant.now()
        );
    }

    @Test
    @DisplayName("Test : recherche par ville seule → résultats")
    void search_byCityOnly_returnsResults() {
        Pageable pageable = PageRequest.of(0, 10);
        VehicleFilter filter = new VehicleFilter(
                null, null, "Casablanca", null, null, null, null, null, null, null, null
        );

        Vehicle vehicle = sampleVehicle();
        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(vehicle), pageable, 1));
        when(mapper.toResponse(vehicle)).thenReturn(sampleResponse());

        Page<VehicleResponse> result = searchService.search(filter, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).ville()).isEqualTo("Casablanca");

        verify(repository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Test : recherche avec dates → valide les dates et retourne les véhicules disponibles")
    void search_withDates_validatesAndReturnsAvailableVehicles() {
        Pageable pageable = PageRequest.of(0, 10);
        VehicleFilter filter = new VehicleFilter(
                null, null, "Casablanca", null, null, null, null,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5),
                null, null
        );

        Vehicle vehicle = sampleVehicle();
        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(vehicle), pageable, 1));
        when(mapper.toResponse(vehicle)).thenReturn(sampleResponse());

        Page<VehicleResponse> result = searchService.search(filter, pageable);

        assertThat(result).isNotEmpty();
        assertThat(result.getContent().get(0).id()).isEqualTo(10L);
        verify(repository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Test : recherche avec dates invalides (dateFin < dateDebut) → exception")
    void search_withInvalidDates_throwsException() {
        Pageable pageable = PageRequest.of(0, 10);
        VehicleFilter filter = new VehicleFilter(
                null, null, "Casablanca", null, null, null, null,
                LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 1),
                null, null
        );

        assertThatThrownBy(() -> searchService.search(filter, pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateFin doit être postérieure ou égale à dateDebut");

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Test : recherche avec dateDebut seule → exception")
    void search_withStartDateOnly_throwsException() {
        Pageable pageable = PageRequest.of(0, 10);
        VehicleFilter filter = new VehicleFilter(
                null, null, "Casablanca", null, null, null, null,
                LocalDate.of(2026, 11, 1), null,
                null, null
        );

        assertThatThrownBy(() -> searchService.search(filter, pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDebut et dateFin doivent être fournies ensemble");

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Test : recherche sans ville → exception")
    void search_withoutCity_throwsException() {
        Pageable pageable = PageRequest.of(0, 10);
        VehicleFilter filter = new VehicleFilter(
                null, null, null, null, null, null, null, null, null, null, null
        );

        assertThatThrownBy(() -> searchService.search(filter, pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La ville est obligatoire");

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Test : recherche avec catégorie → filtre correct")
    void search_withCategory_appliesFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        VehicleFilter filter = new VehicleFilter(
                null, "CITADINE", "Casablanca", null, null, null, null, null, null, null, null
        );

        Vehicle vehicle = sampleVehicle();
        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(vehicle), pageable, 1));
        when(mapper.toResponse(vehicle)).thenReturn(sampleResponse());

        Page<VehicleResponse> result = searchService.search(filter, pageable);

        assertThat(result.getContent().get(0).categorie()).isEqualTo("CITADINE");
        verify(repository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Test : recherche avec prixMax < prixMin → exception")
    void search_withInvalidPrices_throwsException() {
        Pageable pageable = PageRequest.of(0, 10);
        VehicleFilter filter = new VehicleFilter(
                null, null, "Casablanca", null, null, null, null, null, null,
                new BigDecimal("500"), new BigDecimal("200")
        );

        assertThatThrownBy(() -> searchService.search(filter, pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("prixMax doit être supérieur ou égal à prixMin");

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Test : invalidation du cache de recherche")
    void invalidateCache_executesSuccessfully() {
        searchService.invalidateCache();
        // Méthode avec @CacheEvict(value = "vehicleSearch", allEntries = true)
        verifyNoInteractions(repository);
    }
}

