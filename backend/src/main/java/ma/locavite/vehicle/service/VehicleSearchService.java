package ma.locavite.vehicle.service;

import ma.locavite.vehicle.dto.VehicleFilter;
import ma.locavite.vehicle.dto.VehicleResponse;
import ma.locavite.vehicle.mapper.VehicleMapper;
import ma.locavite.vehicle.repository.VehicleRepository;
import ma.locavite.vehicle.spec.VehicleSpecifications;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleSearchService {

    private final VehicleRepository repository;
    private final VehicleMapper mapper;

    public VehicleSearchService(VehicleRepository repository, VehicleMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * Recherche de véhicules avec filtres multi-critères, pagination et mise en cache Redis (TTL 5 min).
     * Si les dates sont fournies, elles sont validées (exclusion des véhicules réservés au Sprint 3).
     * Si les dates sont absentes, retourne tous les véhicules PUBLISHED de la ville correspondant aux critères.
     */
    @Cacheable(value = "vehicleSearch", key = "#filter.cacheKey(#pageable)")
    @Transactional(readOnly = true)
    public Page<VehicleResponse> search(VehicleFilter filter, Pageable pageable) {
        validateFilter(filter);
        return repository.findAll(VehicleSpecifications.publicSearch(filter), pageable)
                .map(mapper::toResponse);
    }

    /**
     * Invalide le cache de recherche de véhicules.
     * Doit être appelé à chaque réservation confirmée (Sprint 3) ainsi qu'en cas de modification de véhicule.
     */
    @CacheEvict(value = "vehicleSearch", allEntries = true)
    public void invalidateCache() {
        // Appelé lors de la confirmation d'une réservation pour vider le cache de recherche.
    }

    private static void validateFilter(VehicleFilter filter) {
        if (filter == null || filter.ville() == null || filter.ville().isBlank()) {
            throw new IllegalArgumentException("La ville est obligatoire");
        }
        if ((filter.dateDebut() == null) != (filter.dateFin() == null)) {
            throw new IllegalArgumentException("dateDebut et dateFin doivent être fournies ensemble");
        }
        if (filter.dateDebut() != null && filter.dateFin().isBefore(filter.dateDebut())) {
            throw new IllegalArgumentException("dateFin doit être postérieure ou égale à dateDebut");
        }
        if (filter.prixMin() != null && filter.prixMax() != null
                && filter.prixMax().compareTo(filter.prixMin()) < 0) {
            throw new IllegalArgumentException("prixMax doit être supérieur ou égal à prixMin");
        }
    }
}
