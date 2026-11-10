package ma.locavite.reservation.repository;

import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.id = :id")
    Optional<Reservation> findByIdForUpdate(@Param("id") Long id);

    Optional<Reservation> findByCode(String code);

    boolean existsByCode(String code);

    Page<Reservation> findByUserId(Long userId, Pageable pageable);

    @Query("""
        SELECT COUNT(r) FROM Reservation r
        WHERE r.vehicleId = :vehicleId
          AND r.statut IN (ma.locavite.reservation.domain.ReservationStatus.PENDING,
                           ma.locavite.reservation.domain.ReservationStatus.CONFIRMED,
                           ma.locavite.reservation.domain.ReservationStatus.ACTIVE)
          AND r.dateDebut < :dateFin
          AND r.dateFin > :dateDebut
          AND (:excludedId IS NULL OR r.id <> :excludedId)
    """)
    long countOverlapping(
            @Param("vehicleId") Long vehicleId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin,
            @Param("excludedId") Long excludedId
    );

    default long countOverlapping(Long vehicleId, LocalDate dateDebut, LocalDate dateFin) {
        return countOverlapping(vehicleId, dateDebut, dateFin, null);
    }

    @Query("""
        SELECT r FROM Reservation r
        WHERE r.vehicleId = :vehicleId
          AND r.statut IN (ma.locavite.reservation.domain.ReservationStatus.PENDING,
                           ma.locavite.reservation.domain.ReservationStatus.CONFIRMED,
                           ma.locavite.reservation.domain.ReservationStatus.ACTIVE)
          AND r.dateDebut < :dateFin
          AND r.dateFin > :dateDebut
          AND (:excludedId IS NULL OR r.id <> :excludedId)
    """)
    List<Reservation> findOverlapping(
            @Param("vehicleId") Long vehicleId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin,
            @Param("excludedId") Long excludedId
    );

    default List<Reservation> findOverlapping(Long vehicleId, LocalDate dateDebut, LocalDate dateFin) {
        return findOverlapping(vehicleId, dateDebut, dateFin, null);
    }

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
