package ma.locavite.reservation.repository;

import ma.locavite.reservation.domain.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    Optional<Refund> findByReservationId(Long reservationId);
}
