package ma.locavite.reservation.repository;

import ma.locavite.reservation.domain.OptionPrice;
import ma.locavite.reservation.domain.ReservationOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OptionPriceRepository extends JpaRepository<OptionPrice, Long> {
    Optional<OptionPrice> findByType(ReservationOption type);
}
