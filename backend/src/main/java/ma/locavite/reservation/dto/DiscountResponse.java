package ma.locavite.reservation.dto;

import ma.locavite.reservation.domain.Discount;
import ma.locavite.reservation.domain.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record DiscountResponse(
        Long id,
        DiscountType type,
        String code,
        String libelle,
        Integer minDays,
        BigDecimal pourcentage,
        LocalDate dateDebut,
        LocalDate dateFin,
        Boolean actif,
        Instant createdAt,
        Instant updatedAt
) {
    public static DiscountResponse fromEntity(Discount entity) {
        return new DiscountResponse(
                entity.getId(),
                entity.getType(),
                entity.getCode(),
                entity.getLibelle(),
                entity.getMinDays(),
                entity.getPourcentage(),
                entity.getDateDebut(),
                entity.getDateFin(),
                entity.getActif(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
