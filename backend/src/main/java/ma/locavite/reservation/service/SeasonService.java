package ma.locavite.reservation.service;

import ma.locavite.common.config.RedisConfig;
import ma.locavite.reservation.domain.Season;
import ma.locavite.reservation.repository.SeasonRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SeasonService {

    private final SeasonRepository seasonRepository;

    public SeasonService(SeasonRepository seasonRepository) {
        this.seasonRepository = seasonRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = RedisConfig.CACHE_SEASONS, key = "'all'")
    public List<Season> getAll() {
        return seasonRepository.findAll();
    }
}
