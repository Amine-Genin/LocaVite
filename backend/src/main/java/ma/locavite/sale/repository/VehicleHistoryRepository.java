package ma.locavite.sale.repository;

import ma.locavite.sale.domain.VehicleHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleHistoryRepository extends JpaRepository<VehicleHistory, Long> {

    List<VehicleHistory> findBySaleListingIdOrderByDateAscIdAsc(Long saleListingId);
}
