package ma.locavite.reservation.dto;

import ma.locavite.reservation.domain.RefundStatus;
import ma.locavite.reservation.domain.ReservationStatus;

import java.math.BigDecimal;

public record CancellationResponse(
        Long reservationId,
        ReservationStatus statut,
        BigDecimal montantRemboursement,
        BigDecimal pourcentageRemboursement,
        RefundStatus statutRemboursement,
        String avoirPath
) {}
