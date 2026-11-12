package ma.locavite.reservation.service;

import ma.locavite.common.config.RedisConfig;
import ma.locavite.reservation.domain.Season;
import ma.locavite.reservation.dto.SeasonRequest;
import ma.locavite.reservation.dto.SeasonResponse;
import ma.locavite.reservation.event.SeasonChangedEvent;
import ma.locavite.reservation.exception.SeasonNotFoundException;
import ma.locavite.reservation.repository.SeasonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SeasonService {

    private static final Logger log = LoggerFactory.getLogger(SeasonService.class);

    private final SeasonRepository seasonRepository;
    private final ApplicationEventPublisher eventPublisher;

    @org.springframework.beans.factory.annotation.Autowired
    public SeasonService(SeasonRepository seasonRepository, ApplicationEventPublisher eventPublisher) {
        this.seasonRepository = seasonRepository;
        this.eventPublisher = eventPublisher;
    }

    public SeasonService(SeasonRepository seasonRepository) {
        this(seasonRepository, null);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_SEASONS, key = "'all'")
    public List<Season> getAll() {
        return seasonRepository.findAllOrderedByPriority();
    }

    @Transactional(readOnly = true)
    public List<SeasonResponse> getAllResponses() {
        return getAll().stream().map(SeasonResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<SeasonResponse> getApplicable(Long agenceId, Long vehicleId) {
        return seasonRepository.findApplicableSeasons(agenceId, vehicleId).stream()
                .map(SeasonResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Season getById(Long id) {
        return seasonRepository.findById(id)
                .orElseThrow(() -> new SeasonNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public SeasonResponse getResponseById(Long id) {
        return SeasonResponse.fromEntity(getById(id));
    }

    @Transactional
    @CacheEvict(value = RedisConfig.CACHE_SEASONS, allEntries = true)
    public SeasonResponse create(SeasonRequest request) {
        validateDates(request);

        Season season = new Season(
                request.getNom(),
                request.getDateDebut(),
                request.getDateFin(),
                request.getCoefficient(),
                request.getPriorite()
        );
        season.setAgenceId(request.getAgenceId());
        season.setVehicleId(request.getVehicleId());
        season.setIsRecurring(request.getIsRecurring());

        Season saved = seasonRepository.save(season);
        log.info("Saison tarifaire créée avec succès [id={}, nom={}, coeff={}]",
                saved.getId(), saved.getNom(), saved.getCoefficient());

        publishChangeEvent(saved, "CREATED");
        return SeasonResponse.fromEntity(saved);
    }

    @Transactional
    @CacheEvict(value = RedisConfig.CACHE_SEASONS, allEntries = true)
    public SeasonResponse update(Long id, SeasonRequest request) {
        validateDates(request);

        Season existing = getById(id);
        existing.setNom(request.getNom());
        existing.setName(request.getNom());
        existing.setDateDebut(request.getDateDebut());
        existing.setDateFin(request.getDateFin());
        existing.setCoefficient(request.getCoefficient());
        existing.setPriorite(request.getPriorite());
        existing.setAgenceId(request.getAgenceId());
        existing.setVehicleId(request.getVehicleId());
        existing.setIsRecurring(request.getIsRecurring());

        Season updated = seasonRepository.save(existing);
        log.info("Saison tarifaire mise à jour avec succès [id={}, nom={}, coeff={}]",
                updated.getId(), updated.getNom(), updated.getCoefficient());

        publishChangeEvent(updated, "UPDATED");
        return SeasonResponse.fromEntity(updated);
    }

    @Transactional
    @CacheEvict(value = RedisConfig.CACHE_SEASONS, allEntries = true)
    public void delete(Long id) {
        Season existing = getById(id);
        seasonRepository.delete(existing);
        log.info("Saison tarifaire supprimée [id={}]", id);
        publishChangeEvent(existing, "DELETED");
    }

    private void validateDates(SeasonRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La requête ne peut pas être nulle");
        }
        if (request.getDateDebut() == null || request.getDateFin() == null) {
            throw new IllegalArgumentException("Les dates de début et de fin sont obligatoires");
        }
        if (!request.getDateDebut().isBefore(request.getDateFin())) {
            throw new IllegalArgumentException("La date de début doit être strictement antérieure à la date de fin");
        }
    }

    private void publishChangeEvent(Season season, String action) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new SeasonChangedEvent(season, action));
        }
    }
}
