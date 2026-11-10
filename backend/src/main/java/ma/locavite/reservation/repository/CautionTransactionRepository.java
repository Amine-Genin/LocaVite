package ma.locavite.reservation.repository;

import ma.locavite.reservation.domain.CautionTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CautionTransactionRepository extends JpaRepository<CautionTransaction, Long> {

    List<CautionTransaction> findByReservationIdOrderByDateDesc(Long reservationId);

    List<CautionTransaction> findByReservationUserIdOrderByDateDesc(Long userId);
}
