package ma.locavite.reservation.repository;

import ma.locavite.reservation.domain.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("reservationRefundRepository")
public interface RefundRepository extends JpaRepository<Refund, Long> {
    Optional<Refund> findByReservationId(Long reservationId);
}
