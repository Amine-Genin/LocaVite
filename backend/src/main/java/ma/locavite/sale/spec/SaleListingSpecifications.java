package ma.locavite.sale.spec;

import jakarta.persistence.criteria.Predicate;
import ma.locavite.sale.domain.SaleListing;
import ma.locavite.sale.domain.SaleStatus;
import ma.locavite.sale.dto.SaleListingFilter;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class SaleListingSpecifications {

    private SaleListingSpecifications() {}

    public static Specification<SaleListing> search(SaleListingFilter filter, boolean onlyPublished) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Exclusion stricte des annonces supprimées (soft delete)
            predicates.add(cb.isFalse(root.get("deleted")));

            if (onlyPublished) {
                predicates.add(cb.equal(root.get("statut"), SaleStatus.PUBLISHED));
            } else if (filter != null && filter.statut() != null) {
                predicates.add(cb.equal(root.get("statut"), filter.statut()));
            }

            if (filter != null) {
                if (filter.etat() != null) {
                    predicates.add(cb.equal(root.get("etat"), filter.etat()));
                }
                if (filter.prixMin() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("prixVente"), filter.prixMin()));
                }
                if (filter.prixMax() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("prixVente"), filter.prixMax()));
                }
                if (filter.kilometrageMax() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("kilometrage"), filter.kilometrageMax()));
                }
                if (filter.vehicleId() != null) {
                    predicates.add(cb.equal(root.get("vehicle").get("id"), filter.vehicleId()));
                }
                if (filter.agenceId() != null) {
                    predicates.add(cb.equal(root.get("agenceId"), filter.agenceId()));
                }
                if (filter.search() != null && !filter.search().isBlank()) {
                    String pattern = "%" + filter.search().trim().toLowerCase() + "%";
                    Predicate titleMatch = cb.like(cb.lower(root.get("titre")), pattern);
                    Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                    predicates.add(cb.or(titleMatch, descMatch));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
