package ma.locavite.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ma.locavite.payment.domain.RefundMethod;

import java.math.BigDecimal;

public record RefundRequest(
        @NotNull(message = "Le montant du remboursement est obligatoire")
        @DecimalMin(value = "0.01", inclusive = true, message = "Le montant du remboursement doit être strictement positif")
        BigDecimal montant,

        @NotBlank(message = "La raison du remboursement est obligatoire")
        @Size(max = 255, message = "La raison ne peut pas dépasser 255 caractères")
        String raison,

        RefundMethod methode
) {
    public RefundRequest(BigDecimal montant, String raison) {
        this(montant, raison, null);
    }
}
