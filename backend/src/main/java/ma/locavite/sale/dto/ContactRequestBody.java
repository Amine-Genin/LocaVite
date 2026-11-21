package ma.locavite.sale.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactRequestBody(
        @NotBlank @Size(max = 200) String nom,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 50) String telephone,
        @NotBlank @Size(max = 5000) String message
) {
}
