package ma.locavite.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ma.locavite.payment.domain.PaymentMethod;
import ma.locavite.payment.domain.PaymentStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

public record PaymentFilter(
        @Schema(description = "Filtrer par identifiant de réservation")
        Long reservationId,

        @Schema(description = "Filtrer par identifiant utilisateur")
        Long userId,

        @Schema(description = "Filtrer par statut de paiement")
        PaymentStatus statut,

        @Schema(description = "Filtrer par méthode de paiement")
        PaymentMethod methode,

        @Schema(description = "Filtrer par code de paiement")
        String code,

        @Schema(description = "Filtrer par référence")
        String reference,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @Schema(description = "Date création minimale")
        Instant dateDebut,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @Schema(description = "Date création maximale")
        Instant dateFin
) {}
