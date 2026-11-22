package ma.locavite.sale.dto;

import java.math.BigDecimal;

public record PriceEstimateResponse(
        BigDecimal estimatedPrice,
        BigDecimal minimumPrice,
        BigDecimal maximumPrice,
        int comparableListings
) {}
