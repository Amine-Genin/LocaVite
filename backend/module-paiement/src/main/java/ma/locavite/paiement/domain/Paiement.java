package ma.locavite.paiement.domain;

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
public class Paiement {

    private Long id;
    private Long reservationId;
    private BigDecimal montant;
    private String methode;
    private String referenceTransaction;

    @Builder.Default
    private PaiementStatus statut = PaiementStatus.PENDING;

    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();
}
