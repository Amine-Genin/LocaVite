package ma.locavite.sale.spec;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import ma.locavite.catalog.domain.Brand;
import ma.locavite.catalog.domain.Category;
import ma.locavite.sale.domain.SaleListing;
import ma.locavite.sale.domain.SaleStatus;
import ma.locavite.sale.domain.VehicleCondition;
import ma.locavite.sale.dto.SaleListingFilter;
import ma.locavite.sale.dto.SaleListingSearchFilter;
import ma.locavite.vehicle.domain.Vehicle;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    public static Specification<SaleListing> publicSearch(SaleListingSearchFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));
            predicates.add(cb.equal(root.get("statut"), SaleStatus.PUBLISHED));

            if (filter == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            Join<SaleListing, Vehicle> vehicle = root.join("vehicle", JoinType.INNER);
            if (filter.etat() != null) {
                predicates.add(cb.equal(root.get("etat"), filter.etat()));
            }
            if (hasText(filter.marque())) {
                Join<Vehicle, Brand> brand = vehicle.join("brand", JoinType.INNER);
                predicates.add(containsIgnoreCase(cb, brand.get("name"), filter.marque()));
            }
            if (hasText(filter.modele())) {
                predicates.add(containsIgnoreCase(cb, vehicle.get("modele"), filter.modele()));
            }
            if (hasText(filter.categorie())) {
                Join<Vehicle, Category> category = vehicle.join("category", JoinType.INNER);
                predicates.add(containsIgnoreCase(cb, category.get("name"), filter.categorie()));
            }
            if (filter.carburant() != null) {
                predicates.add(cb.equal(vehicle.get("carburant"), filter.carburant()));
            }
            if (filter.transmission() != null) {
                predicates.add(cb.equal(vehicle.get("transmission"), filter.transmission()));
            }
            if (filter.anneeMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(vehicle.get("annee"), filter.anneeMin()));
            }
            if (filter.anneeMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(vehicle.get("annee"), filter.anneeMax()));
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
            if (hasText(filter.ville())) {
                predicates.add(containsIgnoreCase(cb, vehicle.get("ville"), filter.ville()));
            }
            if (filter.premierProprietaire() != null) {
                predicates.add(cb.equal(root.get("premierProprietaire"), filter.premierProprietaire()));
            }
            if (filter.garantie() != null) {
                VehicleCondition guaranteedCondition = filter.garantie()
                        ? VehicleCondition.NEW
                        : VehicleCondition.USED;
                predicates.add(cb.equal(root.get("etat"), guaranteedCondition));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static Predicate containsIgnoreCase(
            jakarta.persistence.criteria.CriteriaBuilder cb,
            jakarta.persistence.criteria.Expression<String> expression,
            String value) {
        String pattern = "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
        return cb.like(cb.lower(expression), pattern);
    }
}
