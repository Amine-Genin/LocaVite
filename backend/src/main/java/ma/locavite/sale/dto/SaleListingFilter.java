package ma.locavite.sale.dto;

import ma.locavite.sale.domain.SaleStatus;
import ma.locavite.sale.domain.VehicleCondition;

import java.math.BigDecimal;

public record SaleListingFilter(
        String search,
        VehicleCondition etat,
        SaleStatus statut,
        BigDecimal prixMin,
        BigDecimal prixMax,
        Integer kilometrageMax,
        Long vehicleId,
        Long agenceId
) {}
