package ma.locavite.reservation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "locavite.pricing")
public class PricingProperties {

    private BigDecimal vatRate;

    public BigDecimal getVatRate() {
        return vatRate;
    }

    public void setVatRate(BigDecimal vatRate) {
        this.vatRate = vatRate;
    }

    public BigDecimal requireVatRate() {
        if (vatRate == null || vatRate.signum() < 0 || vatRate.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalStateException("Le taux de TVA doit être configuré entre 0 et 100 dans locavite.pricing.vat-rate");
        }
        return vatRate;
    }
}
