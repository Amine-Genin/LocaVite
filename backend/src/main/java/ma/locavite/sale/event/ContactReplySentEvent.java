package ma.locavite.sale.event;

import ma.locavite.sale.domain.ContactRequest;
import ma.locavite.sale.domain.SaleListing;

public record ContactReplySentEvent(ContactRequest contactRequest, SaleListing listing, String replyMessage) {
}
