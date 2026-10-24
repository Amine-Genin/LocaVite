package ma.locavite.vehicules.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicule {

    private Long id;
    private String marque;
    private String modele;
    private Integer annee;
    private BigDecimal prixJournalier;

    @Builder.Default
    private Boolean disponible = true;
}
