package ma.locavite.vente.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnonceVente {

    private Long id;
    private Long vehiculeId;
    private String titre;
    private String description;
    private BigDecimal prixVente;

    @Builder.Default
    private Boolean active = true;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}
