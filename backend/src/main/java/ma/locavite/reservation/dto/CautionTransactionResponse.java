package ma.locavite.reservation.dto;

import ma.locavite.reservation.domain.CautionStatus;
import ma.locavite.reservation.domain.CautionTransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record CautionTransactionResponse(
        Long id,
        Long reservationId,
        BigDecimal montant,
        CautionTransactionType type,
        CautionStatus statut,
        Instant date,
        String note
) {}
