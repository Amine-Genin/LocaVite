package ma.locavite.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

import java.math.BigDecimal;

@Entity
@Table(name = "categories")
public class Category extends CatalogEntity {

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal caution = new BigDecimal("3000.00");

    public BigDecimal getCaution() { return caution; }
    public void setCaution(BigDecimal caution) { this.caution = caution; }
}
