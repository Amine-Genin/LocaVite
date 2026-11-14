package ma.locavite.payment.cmi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Schema(description = "Réponse d'initiation d'un paiement CMI contenant les paramètres et l'URL de redirection")
public record CmiInitiateResponse(
        @Schema(description = "ID unique du paiement créé dans LocaVite", example = "42")
        @JsonProperty("paymentId") Long paymentId,

        @Schema(description = "Code métier unique du paiement", example = "PAY-CMI789")
        String paymentCode,

        @Schema(description = "Montant du paiement", example = "750.00")
        BigDecimal montant,

        @Schema(description = "Devise du paiement (par défaut MAD)", example = "MAD")
        String devise,

        @Schema(description = "URL de la passerelle CMI vers laquelle rediriger le client", example = "https://testpayment.cmi.co.ma/fim/est3Dgate")
        String gatewayUrl,

        @Schema(description = "URL complète de redirection CMI", example = "https://testpayment.cmi.co.ma/fim/est3Dgate")
        String redirectUrl,

        @Schema(description = "Ensemble des champs du formulaire CMI signés avec HMAC-SHA512 / SHA-512")
        Map<String, String> formData,

        @Schema(description = "Date et heure d'expiration de la session CMI (TTL 15 min)")
        Instant expiresAt
) {
}
