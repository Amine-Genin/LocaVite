package ma.locavite.payment.repository;

import ma.locavite.payment.domain.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByPaymentId(Long paymentId);

    Optional<Invoice> findByNumero(String numero);

    boolean existsByPaymentId(Long paymentId);

    @Query(value = "SELECT nextval('invoice_number_seq')", nativeQuery = true)
    Long getNextSequenceValue();
}
