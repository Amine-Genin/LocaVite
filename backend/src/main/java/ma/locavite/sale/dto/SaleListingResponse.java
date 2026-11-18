package ma.locavite.sale.dto;

import ma.locavite.sale.domain.SaleStatus;
import ma.locavite.sale.domain.VehicleCondition;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record SaleListingResponse(
        Long id,
        String code,
        Long vehicleId,
        Long agenceId,
        String titre,
        String description,
        BigDecimal prixVente,
        Boolean prixNego,
        VehicleCondition etat,
        Integer kilometrage,
        Boolean premierProprietaire,
        LocalDate dateMiseEnCirculation,
        String numeroChassis,
        Integer nombreProprietaires,
        SaleStatus statut,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt,
        Instant soldAt
) implements Serializable {}
