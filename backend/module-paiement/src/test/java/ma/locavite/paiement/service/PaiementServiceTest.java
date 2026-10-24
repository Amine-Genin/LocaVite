package ma.locavite.paiement.service;

import ma.locavite.paiement.domain.Paiement;
import ma.locavite.paiement.domain.PaiementStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PaiementServiceTest {

    private final PaiementService paiementService = new PaiementService();

    @Test
    @DisplayName("Devrait initier un paiement avec référence générée et statut PENDING")
    void shouldInitiatePayment() {
        Paiement p = Paiement.builder()
                .reservationId(10L)
                .montant(new BigDecimal("500.00"))
                .methode("CARTE_BANCAIRE")
                .build();

        Paiement result = paiementService.initierPaiement(p);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getStatut()).isEqualTo(PaiementStatus.PENDING);
        assertThat(result.getReferenceTransaction()).isNotNull().startsWith("TX-");

        Optional<Paiement> found = paiementService.getPaiementById(result.getId());
        assertThat(found).isPresent();
    }
}
