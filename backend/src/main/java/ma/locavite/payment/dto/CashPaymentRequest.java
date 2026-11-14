package ma.locavite.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import ma.locavite.payment.domain.PaymentMethod;

public record CashPaymentRequest(
        @NotNull(message = "L'identifiant de la réservation est obligatoire")
        @Schema(description = "Identifiant de la réservation", example = "1")
        Long reservationId,

        @NotNull(message = "La méthode de paiement est obligatoire")
        @Schema(description = "Méthode de paiement en espèces (CASH_DELIVERY ou CASH_AGENCY)", example = "CASH_DELIVERY")
        PaymentMethod methode
) {}
