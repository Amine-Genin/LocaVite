package ma.locavite.payment.repository;

import ma.locavite.payment.domain.Refund;
import ma.locavite.payment.domain.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findByPaymentIdOrderByDateDesc(Long paymentId);

    List<Refund> findByPaymentId(Long paymentId);

    List<Refund> findByPaymentIdAndStatut(Long paymentId, RefundStatus statut);

    @Query("SELECT COALESCE(SUM(r.montant), 0) FROM Refund r WHERE r.paymentId = :paymentId AND r.statut = ma.locavite.payment.domain.RefundStatus.COMPLETED")
    BigDecimal sumCompletedAmountByPaymentId(@Param("paymentId") Long paymentId);

    @Query(value = "SELECT nextval('avoir_number_seq')", nativeQuery = true)
    Long getNextAvoirSequenceValue();
}
