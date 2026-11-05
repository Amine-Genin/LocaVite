package ma.locavite.vehicle.repository;

import ma.locavite.vehicle.domain.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long>, JpaSpecificationExecutor<Vehicle> {

    boolean existsByMatriculeIgnoreCase(String matricule);

    List<Vehicle> findByAgenceId(Long agenceId);

    @Query("SELECT v.id FROM Vehicle v WHERE v.agenceId = :agenceId")
    List<Long> findIdsByAgenceId(@Param("agenceId") Long agenceId);
}
