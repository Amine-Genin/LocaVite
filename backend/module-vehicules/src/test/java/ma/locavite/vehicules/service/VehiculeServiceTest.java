package ma.locavite.vehicules.service;

import ma.locavite.vehicules.domain.Vehicule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class VehiculeServiceTest {

    private final VehiculeService vehiculeService = new VehiculeService();

    @Test
    @DisplayName("Devrait enregistrer et lister les véhicules disponibles")
    void shouldRegisterAndListAvailableVehicles() {
        Vehicule v = Vehicule.builder()
                .marque("Renault")
                .modele("Clio")
                .annee(2023)
                .prixJournalier(new BigDecimal("350.00"))
                .disponible(true)
                .build();

        Vehicule saved = vehiculeService.enregistrerVehicule(v);
        assertThat(saved.getId()).isNotNull();

        List<Vehicule> result = vehiculeService.getVehiculesDisponibles();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMarque()).isEqualTo("Renault");

        Optional<Vehicule> found = vehiculeService.getVehiculeById(saved.getId());
        assertThat(found).isPresent();
    }
}
