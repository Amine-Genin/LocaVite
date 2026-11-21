package ma.locavite.sale.repository;

import ma.locavite.sale.domain.ContactRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContactRequestRepository extends JpaRepository<ContactRequest, Long> {

    List<ContactRequest> findBySaleListingAgenceIdOrderByCreatedAtDesc(Long agenceId);

    List<ContactRequest> findAllByOrderByCreatedAtDesc();

    Optional<ContactRequest> findByIdAndSaleListingAgenceId(Long id, Long agenceId);
}
