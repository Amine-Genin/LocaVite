package ma.locavite.payment.cmi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Demande d'initiation d'un paiement en ligne via CMI")
public record CmiInitiateRequest(
        @NotNull(message = "L'identifiant de la réservation est obligatoire")
        @Schema(description = "Identifiant technique de la réservation", example = "10")
        Long reservationId,

        @NotNull(message = "Le montant du paiement est obligatoire")
        @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à zéro")
        @Schema(description = "Montant en Dirhams marocains (MAD)", example = "750.00")
        BigDecimal montant,

        @NotBlank(message = "L'URL de retour est obligatoire")
        @Schema(description = "URL de retour applicative après le passage par CMI", example = "https://locavite.ma/reservations/10/confirmation")
        String returnUrl
) {
}
