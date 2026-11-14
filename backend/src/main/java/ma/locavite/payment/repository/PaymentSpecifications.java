package ma.locavite.payment.repository;

import jakarta.persistence.criteria.Predicate;
import ma.locavite.payment.domain.Payment;
import ma.locavite.payment.dto.PaymentFilter;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class PaymentSpecifications {

    private PaymentSpecifications() {}

    public static Specification<Payment> withFilter(PaymentFilter filter, Collection<Long> allowedReservationIds) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (allowedReservationIds != null) {
                if (allowedReservationIds.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("reservationId").in(allowedReservationIds));
                }
            }

            if (filter != null) {
                if (filter.reservationId() != null) {
                    predicates.add(cb.equal(root.get("reservationId"), filter.reservationId()));
                }
                if (filter.userId() != null) {
                    predicates.add(cb.equal(root.get("userId"), filter.userId()));
                }
                if (filter.statut() != null) {
                    predicates.add(cb.equal(root.get("statut"), filter.statut()));
                }
                if (filter.methode() != null) {
                    predicates.add(cb.equal(root.get("methode"), filter.methode()));
                }
                if (filter.code() != null && !filter.code().isBlank()) {
                    predicates.add(cb.equal(cb.lower(root.get("code")), filter.code().trim().toLowerCase()));
                }
                if (filter.reference() != null && !filter.reference().isBlank()) {
                    predicates.add(cb.equal(cb.lower(root.get("reference")), filter.reference().trim().toLowerCase()));
                }
                if (filter.dateDebut() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.dateDebut()));
                }
                if (filter.dateFin() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.dateFin()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
