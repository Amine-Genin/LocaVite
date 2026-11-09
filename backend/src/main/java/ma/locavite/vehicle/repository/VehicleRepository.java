package ma.locavite.vehicle.repository;

import ma.locavite.vehicle.domain.Vehicle;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long>, JpaSpecificationExecutor<Vehicle> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Vehicle v WHERE v.id = :id")
    Optional<Vehicle> findByIdWithLock(@Param("id") Long id);

    boolean existsByMatriculeIgnoreCase(String matricule);

    List<Vehicle> findByAgenceId(Long agenceId);

    @Query("SELECT v.id FROM Vehicle v WHERE v.agenceId = :agenceId")
    List<Long> findIdsByAgenceId(@Param("agenceId") Long agenceId);
}
