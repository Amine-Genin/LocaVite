package ma.locavite.sale.dto;

import ma.locavite.sale.domain.ContactStatus;

import java.time.Instant;

public record ContactRequestResponse(
        Long id,
        Long saleListingId,
        String nom,
        String email,
        String telephone,
        String message,
        ContactStatus statut,
        Instant createdAt,
        Instant repliedAt,
        String replyMessage
) {
}
