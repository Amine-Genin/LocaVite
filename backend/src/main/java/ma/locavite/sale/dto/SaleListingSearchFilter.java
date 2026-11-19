package ma.locavite.sale.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import ma.locavite.sale.domain.VehicleCondition;
import ma.locavite.vehicle.domain.FuelType;
import ma.locavite.vehicle.domain.TransmissionType;

import java.math.BigDecimal;

public record SaleListingSearchFilter(
        VehicleCondition etat,
        String marque,
        String modele,
        String categorie,
        FuelType carburant,
        TransmissionType transmission,
        @PositiveOrZero(message = "L'année minimale doit être positive ou nulle")
        Integer anneeMin,
        @PositiveOrZero(message = "L'année maximale doit être positive ou nulle")
        Integer anneeMax,
        @DecimalMin(value = "0.0", message = "Le prix minimum doit être positif ou nul")
        BigDecimal prixMin,
        @DecimalMin(value = "0.0", message = "Le prix maximum doit être positif ou nul")
        BigDecimal prixMax,
        @PositiveOrZero(message = "Le kilométrage maximal doit être positif ou nul")
        Integer kilometrageMax,
        String ville,
        Boolean premierProprietaire,
        Boolean garantie
) {
    @JsonIgnore
    @AssertTrue(message = "Les bornes minimales doivent être inférieures ou égales aux bornes maximales")
    public boolean isRangesValid() {
        return isRangeValid(anneeMin, anneeMax)
                && (prixMin == null || prixMax == null || prixMin.compareTo(prixMax) <= 0);
    }

    private static boolean isRangeValid(Integer minimum, Integer maximum) {
        return minimum == null || maximum == null || minimum <= maximum;
    }
}
