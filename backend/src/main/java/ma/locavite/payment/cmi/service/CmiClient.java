package ma.locavite.payment.cmi.service;

import ma.locavite.payment.cmi.config.CmiProperties;
import ma.locavite.payment.cmi.exception.CmiUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class CmiClient {

    private static final Logger log = LoggerFactory.getLogger(CmiClient.class);

    private final RestClient cmiRestClient;
    private final CmiProperties cmiProperties;

    public CmiClient(RestClient cmiRestClient, CmiProperties cmiProperties) {
        this.cmiRestClient = cmiRestClient;
        this.cmiProperties = cmiProperties;
    }

    public boolean isGatewayReachable() {
        if (cmiProperties.isFallbackMode()) {
            log.info("Mode fallback CMI activé explicitement par configuration");
            return false;
        }

        if (!cmiProperties.isCheckGatewayHealth()) {
            // Si la vérification active de santé n'est pas activée, la passerelle est considérée opérationnelle
            return true;
        }

        try {
            ResponseEntity<Void> response = cmiRestClient.get()
                    .retrieve()
                    .toBodilessEntity();
            return response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is3xxRedirection();
        } catch (Exception e) {
            log.warn("La passerelle CMI ({}) ne répond pas : {}", cmiProperties.getGatewayUrl(), e.getMessage());
            return false;
        }
    }

    public void ensureGatewayAvailable() {
        if (!isGatewayReachable()) {
            log.error("Passerelle CMI indisponible à l'adresse {}", cmiProperties.getGatewayUrl());
            throw new CmiUnavailableException(
                    "La passerelle de paiement CMI est temporairement indisponible. " +
                    "Veuillez réessayer plus tard ou choisir un autre mode de paiement (virement bancaire ou espèces en agence)."
            );
        }
    }
}
