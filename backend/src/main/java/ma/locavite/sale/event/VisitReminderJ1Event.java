package ma.locavite.sale.event;

import ma.locavite.sale.domain.SaleListing;
import ma.locavite.sale.domain.VisitRequest;

public record VisitReminderJ1Event(VisitRequest visitRequest, SaleListing listing) {
}
