package ma.locavite.reservation.service;

import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationServiceTest {

    private final ReservationService reservationService = new ReservationService();

    @Test
    @DisplayName("Devrait créer une réservation avec statut PENDING")
    void shouldCreateReservationWithPendingStatus() {
        Reservation res = Reservation.builder()
                .userId(1L)
                .vehiculeId(2L)
                .dateDebut(LocalDate.now())
                .dateFin(LocalDate.now().plusDays(3))
                .prixTotal(new BigDecimal("1050.00"))
                .build();

        Reservation result = reservationService.creerReservation(res);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getStatut()).isEqualTo(ReservationStatus.PENDING);

        List<Reservation> userReservations = reservationService.getReservationsByUser(1L);
        assertThat(userReservations).hasSize(1);
    }
}
