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
                .and(textEquals("marque", f.marque()))
                .and(textEquals("ville", f.ville()))
                .and(equalsValue("categorie", f.categorie()))
                .and(equalsValue("carburant", f.carburant()))
                .and(equalsValue("transmission", f.transmission()));
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
}