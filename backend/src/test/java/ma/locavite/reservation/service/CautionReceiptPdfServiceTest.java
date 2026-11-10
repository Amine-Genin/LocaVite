package ma.locavite.reservation.service;

import ma.locavite.reservation.config.ContractProperties;
import ma.locavite.reservation.domain.CautionStatus;
import ma.locavite.reservation.domain.CautionTransaction;
import ma.locavite.reservation.domain.CautionTransactionType;
import ma.locavite.reservation.domain.Reservation;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CautionReceiptPdfServiceTest {

    @Test
    void generate_createsPdfContainingReservationAndMovement() throws IOException {
        Reservation reservation = new Reservation();
        reservation.setCode("LV-ABC123");
        reservation.setCaution(new BigDecimal("3000.00"));
        reservation.setCautionStatut(CautionStatus.HELD);

        CautionTransaction movement = new CautionTransaction();
        movement.setMontant(new BigDecimal("3000.00"));
        movement.setType(CautionTransactionType.HOLD);
        movement.setStatut(CautionStatus.HELD);
        movement.setDate(Instant.parse("2026-11-10T12:00:00Z"));
        movement.setNote("Blocage de la caution");

        byte[] receipt = new CautionReceiptPdfService(
                new PdfFontPathResolver(new ContractProperties()))
                .generate(reservation, List.of(movement));

        try (var pdf = Loader.loadPDF(receipt)) {
            String text = new PDFTextStripper().getText(pdf);
            assertThat(text).contains("LV-ABC123", "3000.00", "HOLD", "Blocage de la caution");
        }
    }
}
