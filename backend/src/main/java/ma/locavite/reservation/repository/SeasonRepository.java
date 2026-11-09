package ma.locavite.reservation.repository;

import ma.locavite.reservation.domain.Season;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonRepository extends JpaRepository<Season, Long> {
}
