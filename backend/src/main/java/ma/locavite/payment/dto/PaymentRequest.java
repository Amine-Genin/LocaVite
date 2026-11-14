package ma.locavite.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import ma.locavite.payment.domain.PaymentMethod;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotNull(message = "L'identifiant de la réservation est obligatoire")
        @Schema(description = "Identifiant de la réservation", example = "1")
        Long reservationId,

        @NotNull(message = "Le montant est obligatoire")
        @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
        @Schema(description = "Montant du paiement", example = "500.00")
        BigDecimal montant,

        @Schema(description = "Devise du paiement (par défaut MAD)", example = "MAD")
        String devise,

        @NotNull(message = "La méthode de paiement est obligatoire")
        @Schema(description = "Méthode de paiement", example = "CMI")
        PaymentMethod methode,

        @Schema(description = "Référence externe (ex: ID CMI)", example = "CMI-REF-123456")
        String reference,

        @Schema(description = "Données additionnelles au format JSON", example = "{\"cmiSessionId\":\"xyz\"}")
        String metadata
) {}
