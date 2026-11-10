package ma.locavite.reservation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ma.locavite.reservation.domain.CautionTransactionType;

import java.math.BigDecimal;

public record CautionRequest(
        @NotNull CautionTransactionType type,
        @NotNull @Positive BigDecimal montant,
        @Size(max = 500) String note
) {}
