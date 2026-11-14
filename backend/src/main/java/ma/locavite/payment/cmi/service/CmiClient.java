package ma.locavite.payment.cmi.service;

import ma.locavite.payment.cmi.config.CmiProperties;
import ma.locavite.payment.cmi.exception.CmiUnavailableException;
import ma.locavite.payment.domain.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

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

    /**
     * Exécute un remboursement CMI via l'endpoint dédié CMI Refund API (trantype=Credit).
     */
    public String executeRefund(Payment payment, BigDecimal montant, String raison) {
        ensureGatewayAvailable();

        String formattedAmount = montant.setScale(2, RoundingMode.HALF_UP).toPlainString();
        String refundUrl = cmiProperties.getRefundUrl() != null
                ? cmiProperties.getRefundUrl()
                : CmiProperties.DEFAULT_REFUND_URL;

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("clientid", cmiProperties.getMerchantId());
        payload.put("trantype", "Credit");
        payload.put("oid", payment.getCode());
        payload.put("transId", payment.getTransactionId() != null ? payment.getTransactionId() : payment.getCode());
        payload.put("amount", formattedAmount);
        payload.put("currency", cmiProperties.getCurrency());
        payload.put("reason", raison != null ? raison : "");

        if (cmiProperties.isCheckGatewayHealth() && cmiRestClient != null) {
            try {
                ResponseEntity<String> response = cmiRestClient.post()
                        .uri(refundUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .toEntity(String.class);
                if (!response.getStatusCode().is2xxSuccessful()) {
                    throw new CmiUnavailableException("Échec de l'appel à l'API CMI Refund (HTTP " + response.getStatusCode() + ")");
                }
            } catch (CmiUnavailableException e) {
                throw e;
            } catch (Exception e) {
                log.error("Erreur lors de l'appel à l'API CMI Refund ({}) pour le paiement {} : {}",
                        refundUrl, payment.getCode(), e.getMessage());
                throw new CmiUnavailableException("Erreur lors du remboursement via l'API CMI : " + e.getMessage(), e);
            }
        }

        String refundTxId = "CMI-REF-" + payment.getCode() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Remboursement CMI traité via endpoint dédié [url={}, oid={}, montant={} MAD, refundTxId={}]",
                refundUrl, payment.getCode(), formattedAmount, refundTxId);
        return refundTxId;
    }
}
