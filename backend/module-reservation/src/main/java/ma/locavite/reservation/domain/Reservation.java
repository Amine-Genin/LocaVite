package ma.locavite.reservation.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    private Long id;
    private Long userId;
    private Long vehiculeId;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private BigDecimal prixTotal;

    @Builder.Default
    private ReservationStatus statut = ReservationStatus.PENDING;
}
