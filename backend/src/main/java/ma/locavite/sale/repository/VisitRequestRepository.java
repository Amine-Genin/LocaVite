package ma.locavite.sale.repository;

import ma.locavite.sale.domain.VisitRequest;
import ma.locavite.sale.domain.VisitStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VisitRequestRepository extends JpaRepository<VisitRequest, Long> {

    Optional<VisitRequest> findByIdAndClientId(Long id, Long clientId);

    Optional<VisitRequest> findByIdAndSaleListingAgenceId(Long id, Long agenceId);

    List<VisitRequest> findBySaleListingAgenceIdOrderByCreatedAtDesc(Long agenceId);

    List<VisitRequest> findAllByOrderByCreatedAtDesc();

    List<VisitRequest> findByStatutAndDateSouhaitee(VisitStatus statut, LocalDate dateSouhaitee);

    List<VisitRequest> findByStatutAndDateSouhaiteeAndReminderSentAtIsNull(
            VisitStatus statut,
            LocalDate dateSouhaitee);
}
