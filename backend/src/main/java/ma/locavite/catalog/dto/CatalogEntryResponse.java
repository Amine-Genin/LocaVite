package ma.locavite.catalog.dto;

import java.io.Serializable;

public record CatalogEntryResponse(Long id, String name) implements Serializable {

    private static final long serialVersionUID = 1L;
}
