package ma.locavite.vehicle.repository;
import ma.locavite.vehicle.domain.VehicleMaintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface VehicleMaintenanceRepository extends JpaRepository<VehicleMaintenance, Long> {
    List<VehicleMaintenance> findByVehicleIdOrderByDateDesc(Long vehicleId);
}
