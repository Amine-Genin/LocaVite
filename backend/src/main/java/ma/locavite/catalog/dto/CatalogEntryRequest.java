package ma.locavite.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CatalogEntryRequest(
        @NotBlank @Size(max = 100) String name
) {}
