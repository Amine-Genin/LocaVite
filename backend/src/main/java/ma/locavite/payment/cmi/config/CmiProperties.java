package ma.locavite.payment.cmi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "cmi")
public class CmiProperties {

    public static final String DEFAULT_TEST_GATEWAY = "https://testpayment.cmi.co.ma/fim/est3Dgate";
    public static final String DEFAULT_PROD_GATEWAY = "https://payment.cmi.co.ma/fim/est3Dgate";

    private String merchantId = "locavite_merchant";
    private String terminalId = "locavite_term";
    private String secretKey = "locavite_cmi_secret_key_dev_123456789";
    private String storeKey = "locavite_cmi_secret_key_dev_123456789";
    private String gatewayUrl = DEFAULT_TEST_GATEWAY;
    private String gatewayUrlTest = DEFAULT_TEST_GATEWAY;
    private String gatewayUrlProd = DEFAULT_PROD_GATEWAY;
    private String callbackUrl = "http://localhost:8081/api/payments/cmi/callback";
    private String webhookUrl = "http://localhost:8081/api/payments/cmi/webhook";
    private List<String> ipWhitelist = new ArrayList<>(Arrays.asList(
            "127.0.0.1",
            "0:0:0:0:0:0:0:1",
            "::1",
            "196.200.160.0/24",
            "41.137.0.0/16"
    ));
    private int sessionTtlMinutes = 15;
    private String currency = "504"; // ISO 4217 numeric for MAD
    private String storeType = "3D_PAY_HOSTING";
    private String tranType = "Auth";
    private String lang = "fr";
    private String encoding = "UTF-8";
    private String signatureAlgorithm = "HMAC_SHA512"; // "HMAC_SHA512" or "SHA512"
    private boolean fallbackMode = false;
    private boolean checkGatewayHealth = false;

    public CmiProperties() {
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getStoreKey() {
        return storeKey != null && !storeKey.isBlank() ? storeKey : secretKey;
    }

    public void setStoreKey(String storeKey) {
        this.storeKey = storeKey;
    }

    public String getGatewayUrl() {
        return gatewayUrl;
    }

    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }

    public String getGatewayUrlTest() {
        return gatewayUrlTest;
    }

    public void setGatewayUrlTest(String gatewayUrlTest) {
        this.gatewayUrlTest = gatewayUrlTest;
    }

    public String getGatewayUrlProd() {
        return gatewayUrlProd;
    }

    public void setGatewayUrlProd(String gatewayUrlProd) {
        this.gatewayUrlProd = gatewayUrlProd;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public List<String> getIpWhitelist() {
        return ipWhitelist;
    }

    public void setIpWhitelist(List<String> ipWhitelist) {
        this.ipWhitelist = ipWhitelist;
    }

    public int getSessionTtlMinutes() {
        return sessionTtlMinutes;
    }

    public void setSessionTtlMinutes(int sessionTtlMinutes) {
        this.sessionTtlMinutes = sessionTtlMinutes;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStoreType() {
        return storeType;
    }

    public void setStoreType(String storeType) {
        this.storeType = storeType;
    }

    public String getTranType() {
        return tranType;
    }

    public void setTranType(String tranType) {
        this.tranType = tranType;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public boolean isFallbackMode() {
        return fallbackMode;
    }

    public void setFallbackMode(boolean fallbackMode) {
        this.fallbackMode = fallbackMode;
    }

    public boolean isCheckGatewayHealth() {
        return checkGatewayHealth;
    }

    public void setCheckGatewayHealth(boolean checkGatewayHealth) {
        this.checkGatewayHealth = checkGatewayHealth;
    }
}
