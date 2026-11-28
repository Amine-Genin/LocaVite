package ma.locavite.reservation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import ma.locavite.reservation.domain.ReservationOption;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record PricingSimulateRequest(
        Long vehicleId,

        @DecimalMin(value = "0.01", message = "Le prix journalier doit être strictement positif")
        BigDecimal prixJour,

        @DecimalMin(value = "0.00", message = "La caution doit être positive ou nulle")
        BigDecimal caution,

        Long agenceId,

        @NotNull(message = "La date de début est obligatoire")
        LocalDate dateDebut,

        @NotNull(message = "La date de fin est obligatoire")
        LocalDate dateFin,

        Set<ReservationOption> options,
        String promoCode
) {}
