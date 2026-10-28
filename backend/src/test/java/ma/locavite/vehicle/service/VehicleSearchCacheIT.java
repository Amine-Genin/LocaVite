package ma.locavite.vehicle.service;

import ma.locavite.catalog.domain.Brand;
import ma.locavite.catalog.domain.Category;
import ma.locavite.catalog.repository.BrandRepository;
import ma.locavite.catalog.repository.CategoryRepository;
import ma.locavite.vehicle.domain.FuelType;
import ma.locavite.vehicle.domain.TransmissionType;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.domain.VehicleStatus;
import ma.locavite.vehicle.dto.VehicleFilter;
import ma.locavite.vehicle.dto.VehicleResponse;
import ma.locavite.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "spring.cache.type=simple")
class VehicleSearchCacheIT {

    @Autowired
    private VehicleSearchService searchService;

    @SpyBean
    private VehicleRepository vehicleRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        if (cacheManager.getCache("vehicleSearch") != null) {
            cacheManager.getCache("vehicleSearch").clear();
        }
        vehicleRepository.deleteAll();

        Brand brand = brandRepository.findByNameIgnoreCase("Toyota").orElseGet(() -> {
            Brand b = new Brand();
            b.setName("Toyota");
            return brandRepository.save(b);
        });

        Category category = categoryRepository.findByNameIgnoreCase("SUV").orElseGet(() -> {
            Category c = new Category();
            c.setName("SUV");
            return categoryRepository.save(c);
        });

        Vehicle vehicle = new Vehicle();
        vehicle.setBrand(brand);
        vehicle.setCategory(category);
        vehicle.setModele("RAV4");
        vehicle.setAnnee(2023);
        vehicle.setVille("Casablanca");
        vehicle.setPrixJour(new BigDecimal("450.00"));
        vehicle.setPrixVente(new BigDecimal("280000.00"));
        vehicle.setCarburant(FuelType.HYBRIDE);
        vehicle.setTransmission(TransmissionType.AUTOMATIQUE);
        vehicle.setPlaces(5);
        vehicle.setDescription("SUV hybride en parfait état");
        vehicle.setMatricule("9999-C-1");
        vehicle.setStatut(VehicleStatus.PUBLISHED);
        vehicle.setAgenceId(1L);

        vehicleRepository.saveAndFlush(vehicle);
        reset(vehicleRepository);
    }

    @Test
    @DisplayName("Test : cache → deuxième appel = même résultat sans requête DB, puis invalidation")
    void search_secondCall_hitsCacheWithoutDbQuery() {
        VehicleFilter filter = new VehicleFilter(
                null, null, "Casablanca", null, null, null, null, null, null, null, null
        );
        Pageable pageable = PageRequest.of(0, 10, Sort.by("prixJour").ascending());

        // 1er appel : le cache est vide, appel DB
        Page<VehicleResponse> firstCall = searchService.search(filter, pageable);
        assertThat(firstCall).isNotEmpty();
        assertThat(firstCall.getContent().get(0).ville()).isEqualTo("Casablanca");
        verify(vehicleRepository, times(1)).findAll(any(Specification.class), eq(pageable));

        // 2ème appel avec les mêmes paramètres : doit être servi par le cache sans requête DB
        Page<VehicleResponse> secondCall = searchService.search(filter, pageable);
        assertThat(secondCall).isNotEmpty();
        assertThat(secondCall.getContent().get(0).id()).isEqualTo(firstCall.getContent().get(0).id());
        // Le repository n'a toujours été appelé qu'une seule fois !
        verify(vehicleRepository, times(1)).findAll(any(Specification.class), eq(pageable));

        // Invalidation du cache (simule confirmation d'une réservation)
        searchService.invalidateCache();

        // 3ème appel après invalidation : doit interroger à nouveau la DB
        Page<VehicleResponse> thirdCall = searchService.search(filter, pageable);
        assertThat(thirdCall).isNotEmpty();
        verify(vehicleRepository, times(2)).findAll(any(Specification.class), eq(pageable));
    }
}

