package ma.locavite.reservation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "locavite.contract")
public class ContractProperties {

    private String publicBaseUrl = "http://localhost:8081";
    private String fontPath;

    public String getPublicBaseUrl() { return publicBaseUrl; }
    public void setPublicBaseUrl(String publicBaseUrl) { this.publicBaseUrl = publicBaseUrl; }
    public String getFontPath() { return fontPath; }
    public void setFontPath(String fontPath) { this.fontPath = fontPath; }
}
