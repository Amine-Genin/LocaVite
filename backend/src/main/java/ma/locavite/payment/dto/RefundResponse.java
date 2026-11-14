package ma.locavite.payment.dto;

import ma.locavite.payment.domain.PaymentStatus;
import ma.locavite.payment.domain.RefundMethod;
import ma.locavite.payment.domain.RefundStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record RefundResponse(
        Long id,
        Long paymentId,
        String paymentCode,
        PaymentStatus paymentStatut,
        BigDecimal montant,
        BigDecimal montantTotalRembourse,
        BigDecimal montantRestant,
        String raison,
        RefundStatus statut,
        RefundMethod methode,
        String reference,
        String avoirPath,
        Instant date,
        String auditTrail
) {
}
