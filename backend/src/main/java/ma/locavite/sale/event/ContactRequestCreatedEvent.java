package ma.locavite.sale.event;

import ma.locavite.sale.domain.ContactRequest;
import ma.locavite.sale.domain.SaleListing;

public record ContactRequestCreatedEvent(ContactRequest contactRequest, SaleListing listing) {
}
