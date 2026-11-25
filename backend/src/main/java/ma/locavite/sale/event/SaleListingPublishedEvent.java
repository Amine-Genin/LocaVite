package ma.locavite.sale.event;

import ma.locavite.sale.domain.SaleListing;

public record SaleListingPublishedEvent(SaleListing listing) {
}
