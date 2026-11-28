package ma.locavite.reservation.dto;

import java.math.BigDecimal;

public record PricingSimulateResponse(
        BigDecimal sousTotal,
        BigDecimal options,
        BigDecimal remiseDuree,
        BigDecimal remisePromo,
        BigDecimal remiseTotale,
        BigDecimal caution,
        BigDecimal tva,
        BigDecimal total,
        long jours,
        String promoCodeApplique
) {}
