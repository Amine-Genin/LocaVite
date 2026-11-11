package ma.locavite.reservation.repository;

import ma.locavite.reservation.domain.ReservationExtension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationExtensionRepository extends JpaRepository<ReservationExtension, Long> {

    List<ReservationExtension> findByReservationIdOrderByCreatedAtAsc(Long reservationId);

    List<ReservationExtension> findByReservationIdOrderByCreatedAtDesc(Long reservationId);
}
