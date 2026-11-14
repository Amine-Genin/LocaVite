package ma.locavite.payment.cmi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
public class CmiHashService {

    private static final Logger log = LoggerFactory.getLogger(CmiHashService.class);

    private static final Set<String> EXCLUDED_HASH_KEYS = Set.of(
            "hash",
            "encoding",
            "hashalgorithm",
            "hashdata"
    );

    public String generateInitiateHash(Map<String, String> params, String secretKey, String algorithm) {
        String data = buildSortedData(params);
        if ("SHA512".equalsIgnoreCase(algorithm)) {
            return generateSha512(data + secretKey);
        }
        // Par défaut HMAC-SHA512 selon les critères d'acceptation
        return generateHmacSha512(data, secretKey);
    }

    public String generateSha512(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algorithme SHA-512 non disponible", e);
        }
    }

    public String generateHmacSha512(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Erreur lors de la génération de la signature HMAC-SHA512", e);
        }
    }

    public String buildSortedData(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        List<String> sortedKeys = new ArrayList<>(params.keySet());
        sortedKeys.sort(String.CASE_INSENSITIVE_ORDER);

        StringBuilder sb = new StringBuilder();
        for (String key : sortedKeys) {
            if (isExcluded(key)) {
                continue;
            }
            String value = params.get(key);
            if (value != null) {
                sb.append(escapeValue(value));
            }
        }
        return sb.toString();
    }

    public String buildSortedDataWithSeparator(Map<String, String> params, String separator) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        List<String> sortedKeys = new ArrayList<>(params.keySet());
        sortedKeys.sort(String.CASE_INSENSITIVE_ORDER);

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String key : sortedKeys) {
            if (isExcluded(key)) {
                continue;
            }
            String value = params.get(key);
            if (value != null) {
                if (!first && separator != null) {
                    sb.append(separator);
                }
                sb.append(escapeValue(value));
                first = false;
            }
        }
        return sb.toString();
    }

    public boolean verifyHash(Map<String, String> params, String secretKey) {
        if (params == null || params.isEmpty()) {
            return false;
        }

        String receivedHash = extractHash(params);
        if (receivedHash == null || receivedHash.isBlank()) {
            log.warn("Aucun hash trouvé dans les paramètres CMI fournis");
            return false;
        }

        // 1. Vérification avec HASHPARAMS si fourni par CMI (spécification standard Nestpay / CMI)
        String hashParams = params.get("HASHPARAMS");
        if (hashParams != null && !hashParams.isBlank()) {
            StringBuilder sb = new StringBuilder();
            String[] paramNames = hashParams.split(":");
            for (String paramName : paramNames) {
                String val = params.get(paramName);
                if (val != null) {
                    sb.append(escapeValue(val));
                }
            }
            String dataWithStoreKey = sb + secretKey;
            String computedHashParamsSha512 = generateSha512(dataWithStoreKey);
            if (receivedHash.equalsIgnoreCase(computedHashParamsSha512)) {
                return true;
            }
            String computedHashParamsHmac = generateHmacSha512(sb.toString(), secretKey);
            if (receivedHash.equalsIgnoreCase(computedHashParamsHmac)) {
                return true;
            }
        }

        // 2. Vérification HMAC-SHA512 sur champs triés
        String sortedData = buildSortedData(params);
        String computedHmac = generateHmacSha512(sortedData, secretKey);
        if (receivedHash.equalsIgnoreCase(computedHmac)) {
            return true;
        }

        // 3. Vérification SHA-512 sur concat(champs triés) + secretKey
        String computedSha512 = generateSha512(sortedData + secretKey);
        if (receivedHash.equalsIgnoreCase(computedSha512)) {
            return true;
        }

        // 4. Vérification avec séparateur '|'
        String sortedDataPipe = buildSortedDataWithSeparator(params, "|");
        String computedSha512Pipe = generateSha512(sortedDataPipe + "|" + secretKey);
        if (receivedHash.equalsIgnoreCase(computedSha512Pipe)) {
            return true;
        }

        log.warn("Échec de vérification du hash CMI pour la transaction.");
        return false;
    }

    private String extractHash(Map<String, String> params) {
        if (params.containsKey("HASH")) {
            return params.get("HASH");
        }
        if (params.containsKey("hash")) {
            return params.get("hash");
        }
        if (params.containsKey("hashData")) {
            return params.get("hashData");
        }
        if (params.containsKey("HASHDATA")) {
            return params.get("HASHDATA");
        }
        return null;
    }

    private boolean isExcluded(String key) {
        if (key == null) return true;
        return EXCLUDED_HASH_KEYS.contains(key.toLowerCase());
    }

    private String escapeValue(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("|", "\\|");
    }
}
