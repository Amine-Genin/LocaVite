package ma.locavite.reservation.repository;

import ma.locavite.reservation.domain.ContractLanguage;
import ma.locavite.reservation.domain.ReservationContract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReservationContractRepository extends JpaRepository<ReservationContract, Long> {
    Optional<ReservationContract> findByReservationIdAndLanguage(Long reservationId, ContractLanguage language);

    List<ReservationContract> findByReservationId(Long reservationId);

    Optional<ReservationContract> findByReservationIdAndLanguageAndAccessToken(
            Long reservationId, ContractLanguage language, String accessToken);
}
