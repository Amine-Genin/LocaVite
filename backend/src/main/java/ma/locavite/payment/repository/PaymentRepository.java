package ma.locavite.payment.repository;

import ma.locavite.payment.domain.Payment;
import ma.locavite.payment.domain.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    List<Payment> findByReservationId(Long reservationId);

    List<Payment> findByReservationIdAndStatut(Long reservationId, PaymentStatus statut);

    Page<Payment> findByUserId(Long userId, Pageable pageable);

    Optional<Payment> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT COALESCE(SUM(p.montant), 0) FROM Payment p WHERE p.reservationId = :reservationId AND p.statut = ma.locavite.payment.domain.PaymentStatus.SUCCESS")
    BigDecimal sumSuccessAmountByReservationId(@Param("reservationId") Long reservationId);

    List<Payment> findByReservationIdIn(List<Long> reservationIds);
}
