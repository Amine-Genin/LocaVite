package ma.locavite.payment.cmi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CmiHashServiceTest {

    private CmiHashService hashService;
    private final String secretKey = "test_store_key_123456";

    @BeforeEach
    void setUp() {
        hashService = new CmiHashService();
    }

    @Test
    @DisplayName("Test : génération HMAC-SHA512 cohérente et non vide")
    void generateHmacSha512_producesValidBase64Hash() {
        String data = "amount500.00clientid1234oidPAY001";
        String hash1 = hashService.generateHmacSha512(data, secretKey);
        String hash2 = hashService.generateHmacSha512(data, secretKey);

        assertThat(hash1).isNotNull().isNotBlank();
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("Test : tri insensible à la casse des champs pour concaténation")
    void buildSortedData_sortsFieldsAlphabeticallyCaseInsensitively() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("trantype", "Auth");
        params.put("amount", "500.00");
        params.put("clientid", "1234");
        params.put("oid", "PAY-001");
        params.put("hash", "should_be_ignored");
        params.put("encoding", "UTF-8");

        String sortedData = hashService.buildSortedData(params);

        // Ordre attendu : amount -> clientid -> oid -> trantype
        assertThat(sortedData).isEqualTo("500.001234PAY-001Auth");
    }

    @Test
    @DisplayName("Test : vérification d'un hash HMAC-SHA512 valide")
    void verifyHash_validHmacHash_returnsTrue() {
        Map<String, String> params = new HashMap<>();
        params.put("clientid", "1234");
        params.put("oid", "PAY-001");
        params.put("amount", "500.00");
        params.put("trantype", "Auth");

        String hash = hashService.generateInitiateHash(params, secretKey, "HMAC_SHA512");
        params.put("hash", hash);

        boolean verified = hashService.verifyHash(params, secretKey);
        assertThat(verified).isTrue();
    }

    @Test
    @DisplayName("Test : vérification d'un hash SHA-512 avec secretKey valide")
    void verifyHash_validSha512ConcatHash_returnsTrue() {
        Map<String, String> params = new HashMap<>();
        params.put("clientid", "1234");
        params.put("oid", "PAY-001");
        params.put("amount", "500.00");

        String sortedData = hashService.buildSortedData(params);
        String sha512Hash = hashService.generateSha512(sortedData + secretKey);
        params.put("HASH", sha512Hash);

        boolean verified = hashService.verifyHash(params, secretKey);
        assertThat(verified).isTrue();
    }

    @Test
    @DisplayName("Test : vérification via HASHPARAMS retourné par CMI")
    void verifyHash_withHashParams_returnsTrue() {
        Map<String, String> params = new HashMap<>();
        params.put("clientid", "1234");
        params.put("oid", "PAY-001");
        params.put("ProcReturnCode", "00");
        params.put("Response", "Approved");
        params.put("TransId", "TXN12345");
        params.put("HASHPARAMS", "clientid:oid:ProcReturnCode:Response:TransId");

        String data = "1234" + "PAY-001" + "00" + "Approved" + "TXN12345" + secretKey;
        String hash = hashService.generateSha512(data);
        params.put("HASH", hash);

        boolean verified = hashService.verifyHash(params, secretKey);
        assertThat(verified).isTrue();
    }

    @Test
    @DisplayName("Test : rejet d'un hash altéré ou incorrect")
    void verifyHash_invalidHash_returnsFalse() {
        Map<String, String> params = new HashMap<>();
        params.put("clientid", "1234");
        params.put("oid", "PAY-001");
        params.put("amount", "500.00");
        params.put("hash", "INVALID_HASH_VALUE_XYZ==");

        boolean verified = hashService.verifyHash(params, secretKey);
        assertThat(verified).isFalse();
    }

    @Test
    @DisplayName("Test : rejet si aucun hash présent")
    void verifyHash_missingHash_returnsFalse() {
        Map<String, String> params = new HashMap<>();
        params.put("clientid", "1234");
        params.put("oid", "PAY-001");

        boolean verified = hashService.verifyHash(params, secretKey);
        assertThat(verified).isFalse();
    }
}
