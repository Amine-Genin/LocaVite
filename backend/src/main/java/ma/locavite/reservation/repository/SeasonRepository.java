package ma.locavite.reservation.repository;

import ma.locavite.reservation.domain.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeasonRepository extends JpaRepository<Season, Long> {

    List<Season> findByAgenceId(Long agenceId);

    List<Season> findByVehicleId(Long vehicleId);

    List<Season> findByAgenceIdIsNullAndVehicleIdIsNull();

    @Query("SELECT s FROM Season s ORDER BY s.priorite DESC, s.coefficient DESC")
    List<Season> findAllOrderedByPriority();

    @Query("""
        SELECT s FROM Season s
        WHERE (s.vehicleId = :vehicleId)
           OR (s.agenceId = :agenceId AND s.vehicleId IS NULL)
           OR (s.agenceId IS NULL AND s.vehicleId IS NULL)
        ORDER BY s.priorite DESC, s.coefficient DESC
    """)
    List<Season> findApplicableSeasons(
            @Param("agenceId") Long agenceId,
            @Param("vehicleId") Long vehicleId
    );
}
