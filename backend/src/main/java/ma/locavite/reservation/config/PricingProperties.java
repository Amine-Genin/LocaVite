package ma.locavite.reservation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "locavite.pricing")
public class PricingProperties {

    private BigDecimal vatRate;
    private BigDecimal defaultLowSeasonCoefficient = new BigDecimal("1.00");
    private BigDecimal defaultMediumSeasonCoefficient = new BigDecimal("1.20");
    private BigDecimal defaultHighSeasonCoefficient = new BigDecimal("1.50");
    private BigDecimal defaultVeryHighSeasonCoefficient = new BigDecimal("2.00");

    public BigDecimal getVatRate() {
        return vatRate;
    }

    public void setVatRate(BigDecimal vatRate) {
        this.vatRate = vatRate;
    }

    public BigDecimal getDefaultLowSeasonCoefficient() {
        return defaultLowSeasonCoefficient;
    }

    public void setDefaultLowSeasonCoefficient(BigDecimal defaultLowSeasonCoefficient) {
        this.defaultLowSeasonCoefficient = defaultLowSeasonCoefficient;
    }

    public BigDecimal getDefaultMediumSeasonCoefficient() {
        return defaultMediumSeasonCoefficient;
    }

    public void setDefaultMediumSeasonCoefficient(BigDecimal defaultMediumSeasonCoefficient) {
        this.defaultMediumSeasonCoefficient = defaultMediumSeasonCoefficient;
    }

    public BigDecimal getDefaultHighSeasonCoefficient() {
        return defaultHighSeasonCoefficient;
    }

    public void setDefaultHighSeasonCoefficient(BigDecimal defaultHighSeasonCoefficient) {
        this.defaultHighSeasonCoefficient = defaultHighSeasonCoefficient;
    }

    public BigDecimal getDefaultVeryHighSeasonCoefficient() {
        return defaultVeryHighSeasonCoefficient;
    }

    public void setDefaultVeryHighSeasonCoefficient(BigDecimal defaultVeryHighSeasonCoefficient) {
        this.defaultVeryHighSeasonCoefficient = defaultVeryHighSeasonCoefficient;
    }

    public BigDecimal requireVatRate() {
        if (vatRate == null || vatRate.signum() < 0 || vatRate.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalStateException("Le taux de TVA doit être configuré entre 0 et 100 dans locavite.pricing.vat-rate");
        }
        return vatRate;
    }
}
