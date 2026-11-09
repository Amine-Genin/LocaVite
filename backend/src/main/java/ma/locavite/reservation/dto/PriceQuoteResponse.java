package ma.locavite.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Détail du devis de réservation, en MAD")
public record PriceQuoteResponse(
        @Schema(description = "Prix du véhicule avant remise et options")
        BigDecimal sousTotal,
        @Schema(description = "Total des options pour la durée de location")
        BigDecimal options,
        @Schema(description = "Remise de longue durée")
        BigDecimal remise,
        @Schema(description = "Caution selon la catégorie du véhicule")
        BigDecimal caution,
        @Schema(description = "TVA appliquée au montant locatif après remise, hors caution")
        BigDecimal tva,
        @Schema(description = "Montant total payable, caution comprise")
        BigDecimal total,
        @Schema(description = "Durée de location en jours")
        long jours
) {}
