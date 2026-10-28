package ma.locavite.vehicle.spec;

import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.domain.VehicleStatus;
import ma.locavite.vehicle.dto.VehicleFilter;
import org.springframework.data.jpa.domain.Specification;

public final class VehicleSpecifications {

    private VehicleSpecifications() {}

    /** Recherche publique : uniquement les véhicules PUBLISHED + filtres non nuls. */
    public static Specification<Vehicle> publicSearch(VehicleFilter f) {
        return hasStatus(VehicleStatus.PUBLISHED)
                .and(associationNameEquals("brand", f.marque()))
                .and(idEquals("brand", f.brandId()))
                .and(textEquals("ville", f.ville()))
                .and(associationNameEquals("category", f.categorie()))
                .and(idEquals("category", f.categoryId()))
                .and(equalsValue("carburant", f.carburant()))
                .and(equalsValue("transmission", f.transmission()))
                .and(priceGreaterThanOrEqual(f.prixMin()))
                .and(priceLessThanOrEqual(f.prixMax()));
    }

    private static Specification<Vehicle> hasStatus(VehicleStatus status) {
        return (root, query, cb) -> cb.equal(root.get("statut"), status);
    }

    private static Specification<Vehicle> textEquals(String field, String value) {
        if (value == null || value.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        String v = value.trim().toLowerCase();
        return (root, query, cb) -> cb.equal(cb.lower(root.get(field)), v);
    }

    private static Specification<Vehicle> equalsValue(String field, Object value) {
        if (value == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get(field), value);
    }

    private static Specification<Vehicle> idEquals(String association, Long id) {
        if (id == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get(association).get("id"), id);
    }

    private static Specification<Vehicle> associationNameEquals(String association, String value) {
        if (value == null || value.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        String normalized = value.trim().toLowerCase();
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get(association).get("name")), normalized);
    }

    private static Specification<Vehicle> priceGreaterThanOrEqual(java.math.BigDecimal value) {
        if (value == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("prixJour"), value);
    }

    private static Specification<Vehicle> priceLessThanOrEqual(java.math.BigDecimal value) {
        if (value == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("prixJour"), value);
    }
}