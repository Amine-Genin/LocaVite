package ma.locavite.vente.service;

import ma.locavite.vente.domain.AnnonceVente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VenteServiceTest {

    private final VenteService venteService = new VenteService();

    @Test
    @DisplayName("Devrait publier et lister les annonces de vente actives")
    void shouldPublishAndListActiveAds() {
        AnnonceVente annonce = AnnonceVente.builder()
                .vehiculeId(2L)
                .titre("Dacia Duster 2022")
                .prixVente(new BigDecimal("165000.00"))
                .active(true)
                .build();

        AnnonceVente saved = venteService.publierAnnonce(annonce);
        assertThat(saved.getId()).isNotNull();

        List<AnnonceVente> result = venteService.getAnnoncesActives();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitre()).isEqualTo("Dacia Duster 2022");
    }
}
