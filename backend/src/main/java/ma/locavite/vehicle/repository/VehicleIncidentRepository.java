package ma.locavite.vehicle.repository;
import ma.locavite.vehicle.domain.VehicleIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface VehicleIncidentRepository extends JpaRepository<VehicleIncident, Long> {
    List<VehicleIncident> findByVehicleIdOrderByDateDesc(Long vehicleId);
}
