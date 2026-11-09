package ma.locavite.reservation.repository;

import jakarta.persistence.criteria.Predicate;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.dto.ReservationFilter;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ReservationSpecifications {

    private ReservationSpecifications() {}

    public static Specification<Reservation> withFilter(
            ReservationFilter filter,
            Collection<Long> allowedVehicleIds,
            Long specificUserId) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (allowedVehicleIds != null) {
                if (allowedVehicleIds.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("vehicleId").in(allowedVehicleIds));
                }
            }

            if (specificUserId != null) {
                predicates.add(cb.equal(root.get("userId"), specificUserId));
            }

            if (filter != null) {
                if (filter.userId() != null && specificUserId == null) {
                    predicates.add(cb.equal(root.get("userId"), filter.userId()));
                }
                if (filter.vehicleId() != null) {
                    predicates.add(cb.equal(root.get("vehicleId"), filter.vehicleId()));
                }
                if (filter.statut() != null) {
                    predicates.add(cb.equal(root.get("statut"), filter.statut()));
                }
                if (filter.code() != null && !filter.code().isBlank()) {
                    predicates.add(cb.equal(root.get("code"), filter.code().trim()));
                }
                if (filter.dateDebut() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("dateDebut"), filter.dateDebut()));
                }
                if (filter.dateFin() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("dateFin"), filter.dateFin()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
