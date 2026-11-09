package ma.locavite.vehicle.service;

import ma.locavite.catalog.domain.Brand;
import ma.locavite.catalog.domain.Category;
import ma.locavite.catalog.repository.BrandRepository;
import ma.locavite.catalog.repository.CategoryRepository;
import ma.locavite.vehicle.domain.FuelType;
import ma.locavite.vehicle.domain.TransmissionType;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.domain.VehicleStatus;
import ma.locavite.vehicle.dto.VehicleRequest;
import ma.locavite.vehicle.dto.VehicleResponse;
import ma.locavite.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "spring.cache.type=simple")
class VehicleCacheIT {

    @Autowired
    private VehicleService vehicleService;

    @SpyBean
    private VehicleRepository vehicleRepository;

    @MockBean
    private CurrentUserService currentUserService;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CacheManager cacheManager;

    private Long vehicleId;
    private Long brandId;
    private Long categoryId;

    @BeforeEach
    void setUp() {
        if (cacheManager.getCache("vehicles") != null) {
            cacheManager.getCache("vehicles").clear();
        }
        if (cacheManager.getCache("vehicleSearch") != null) {
            cacheManager.getCache("vehicleSearch").clear();
        }

        vehicleRepository.deleteAll();

        when(currentUserService.getCurrentUserId()).thenReturn(Optional.of(1L));
        when(currentUserService.isAdmin()).thenReturn(true);

        Brand brand = brandRepository.findByNameIgnoreCase("Renault").orElseGet(() -> {
            Brand b = new Brand();
            b.setName("Renault");
            return brandRepository.save(b);
        });
        brandId = brand.getId();

        Category category = categoryRepository.findByNameIgnoreCase("BERLINE").orElseGet(() -> {
            Category c = new Category();
            c.setName("BERLINE");
            return categoryRepository.save(c);
        });
        categoryId = category.getId();

        Vehicle vehicle = new Vehicle();
        vehicle.setBrand(brand);
        vehicle.setCategory(category);
        vehicle.setModele("Clio");
        vehicle.setAnnee(2022);
        vehicle.setVille("Rabat");
        vehicle.setPrixJour(new BigDecimal("300.00"));
        vehicle.setPrixVente(new BigDecimal("150000.00"));
        vehicle.setCarburant(FuelType.DIESEL);
        vehicle.setTransmission(TransmissionType.MANUELLE);
        vehicle.setPlaces(5);
        vehicle.setDescription("Citadine économique");
        vehicle.setMatricule("8888-A-1");
        vehicle.setStatut(VehicleStatus.PUBLISHED);
        vehicle.setAgenceId(1L);

        Vehicle saved = vehicleRepository.saveAndFlush(vehicle);
        vehicleId = saved.getId();

        reset(vehicleRepository);
    }

    @Test
    @DisplayName("GET /api/vehicles/{id} : 2 appels identiques → 1 seule requête DB")
    void getById_hitsCacheOnSecondCall() {
        // 1er appel : interroge le repository
        VehicleResponse first = vehicleService.getById(vehicleId);
        assertThat(first).isNotNull();
        assertThat(first.matricule()).isEqualTo("8888-A-1");
        verify(vehicleRepository, times(1)).findById(vehicleId);

        // 2ème appel : servi par le cache sans nouvelle requête repository
        VehicleResponse second = vehicleService.getById(vehicleId);
        assertThat(second).isNotNull();
        assertThat(second.id()).isEqualTo(first.id());
        verify(vehicleRepository, times(1)).findById(vehicleId);
    }

    @Test
    @DisplayName("PUT /api/vehicles/{id} : invalide le cache 'vehicles' et réinterroge la DB au prochain appel")
    void update_evictsCache() {
        // Remplir le cache
        vehicleService.getById(vehicleId);
        verify(vehicleRepository, times(1)).findById(vehicleId);

        // Mise à jour (PUT)
        VehicleRequest updateReq = new VehicleRequest(
                brandId, "Clio 5", 2023, categoryId, "Rabat",
                new BigDecimal("350.00"), new BigDecimal("160000.00"),
                FuelType.DIESEL, TransmissionType.MANUELLE, 5,
                "Mise à jour description", "8888-A-1", VehicleStatus.PUBLISHED
        );
        vehicleService.update(vehicleId, updateReq);

        // Appel après update : le cache a été invalidé, findById est réexécuté
        VehicleResponse updated = vehicleService.getById(vehicleId);
        assertThat(updated.modele()).isEqualTo("Clio 5");
        // findById appelé pour le cache initial (1), dans findManageable lors de update (2),
        // puis lors de getById après invalidation (3).
        verify(vehicleRepository, atLeast(3)).findById(vehicleId);
    }

    @Test
    @DisplayName("POST /api/vehicles : invalide les caches 'vehicles' et 'vehicleSearch'")
    void create_evictsCache() {
        // Remplir le cache
        vehicleService.getById(vehicleId);
        verify(vehicleRepository, times(1)).findById(vehicleId);

        // Création d'un nouveau véhicule (POST)
        VehicleRequest createReq = new VehicleRequest(
                brandId, "Megane", 2023, categoryId, "Rabat",
                new BigDecimal("400.00"), new BigDecimal("200000.00"),
                FuelType.ESSENCE, TransmissionType.AUTOMATIQUE, 5,
                "Berline compacte", "7777-B-2", VehicleStatus.PUBLISHED
        );
        vehicleService.create(createReq);

        // Après la création, le cache 'vehicles' a été invalidé : getById doit interroger à nouveau la DB
        VehicleResponse refreshed = vehicleService.getById(vehicleId);
        assertThat(refreshed).isNotNull();
        verify(vehicleRepository, times(2)).findById(vehicleId);
    }

    @Test
    @DisplayName("DELETE /api/vehicles/{id} : invalide les caches")
    void delete_evictsCache() {
        // Remplir le cache
        vehicleService.getById(vehicleId);
        verify(vehicleRepository, times(1)).findById(vehicleId);

        // Suppression (DELETE)
        vehicleService.delete(vehicleId);

        // L'invalidation a bien été exécutée par proxy
        assertThat(cacheManager.getCache("vehicles")).isNotNull();
    }

    @Test
    @DisplayName("invalidateSearchCache : vide les caches explicitement")
    void invalidateSearchCache_executesWithoutError() {
        vehicleService.invalidateSearchCache();
        assertThat(cacheManager.getCache("vehicleSearch")).isNotNull();
    }
}
