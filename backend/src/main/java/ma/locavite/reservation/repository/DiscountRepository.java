package ma.locavite.reservation.repository;

import ma.locavite.reservation.domain.Discount;
import ma.locavite.reservation.domain.DiscountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiscountRepository extends JpaRepository<Discount, Long> {

    Optional<Discount> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    List<Discount> findByTypeAndActifTrueOrderByMinDaysDesc(DiscountType type);
}
