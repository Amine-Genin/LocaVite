package ma.locavite.vehicle.repository;

import ma.locavite.vehicle.domain.VehiclePhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehiclePhotoRepository extends JpaRepository<VehiclePhoto, Long> {

    List<VehiclePhoto> findByVehicleIdOrderByOrdreAsc(Long vehicleId);

    Optional<VehiclePhoto> findByIdAndVehicleId(Long id, Long vehicleId);

    long countByVehicleId(Long vehicleId);
}
