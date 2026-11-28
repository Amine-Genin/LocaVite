package ma.locavite.vehicle.repository;
import ma.locavite.vehicle.domain.VehicleStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface VehicleStatusHistoryRepository extends JpaRepository<VehicleStatusHistory, Long> {
    List<VehicleStatusHistory> findByVehicleIdOrderByDateDesc(Long vehicleId);
}
