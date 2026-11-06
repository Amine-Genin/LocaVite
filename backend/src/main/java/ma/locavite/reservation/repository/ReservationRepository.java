package ma.locavite.reservation.repository;

import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    Optional<Reservation> findByCode(String code);

    boolean existsByCode(String code);

    Page<Reservation> findByUserId(Long userId, Pageable pageable);

    @Query("""
        SELECT r FROM Reservation r
        WHERE r.vehicleId = :vehicleId
          AND r.statut IN (:statuses)
          AND r.dateDebut <= :dateFin
          AND r.dateFin >= :dateDebut
          AND (:excludedId IS NULL OR r.id <> :excludedId)
    """)
    List<Reservation> findOverlappingReservations(
            @Param("vehicleId") Long vehicleId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin,
            @Param("statuses") Collection<ReservationStatus> statuses,
            @Param("excludedId") Long excludedId
    );
}
