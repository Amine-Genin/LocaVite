package ma.locavite.payment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import ma.locavite.payment.domain.PaymentMethod;
import ma.locavite.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        @Schema(description = "Identifiant unique du paiement", example = "1")
        Long id,

        @Schema(description = "Code unique du paiement au format PAY-XXXXXX", example = "PAY-A1B2C3")
        String code,

        @Schema(description = "Identifiant de la réservation associée", example = "1")
        Long reservationId,

        @Schema(description = "Identifiant de l'utilisateur ayant payé", example = "42")
        Long userId,

        @Schema(description = "Montant du paiement", example = "500.00")
        BigDecimal montant,

        @Schema(description = "Devise du paiement", example = "MAD")
        String devise,

        @Schema(description = "Méthode de paiement", example = "CMI")
        PaymentMethod methode,

        @Schema(description = "Statut du paiement", example = "PENDING")
        PaymentStatus statut,

        @Schema(description = "Référence externe (ex: ID CMI)", example = "CMI-123456")
        String reference,

        @Schema(description = "Identifiant de transaction", example = "TXN-987654")
        String transactionId,

        @Schema(description = "Métadonnées JSON pour détails CMI", example = "{\"authCode\":\"12345\"}")
        String metadata,

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(description = "Date et heure de création")
        Instant createdAt,

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(description = "Date et heure de dernière mise à jour")
        Instant updatedAt,

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(description = "Date et heure où le paiement a été effectué")
        Instant paidAt
) {}
