package ma.locavite.sale.dto;

import jakarta.validation.constraints.Size;

public record VisitConfirmBody(
        @Size(max = 5000) String notes
) {
}
