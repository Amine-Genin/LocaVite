package ma.locavite.reservation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.locavite.reservation.dto.CautionRequest;
import ma.locavite.reservation.dto.CautionTransactionResponse;
import ma.locavite.reservation.service.CautionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Cautions", description = "Gestion des cautions de réservation")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasAnyRole('AGENCE', 'ADMIN')")
public class AdminCautionController {

    private final CautionService cautionService;

    public AdminCautionController(CautionService cautionService) {
        this.cautionService = cautionService;
    }

    @PutMapping("/reservations/{id}/caution")
    @Operation(summary = "Bloquer, libérer ou retenir la caution d'une réservation")
    public ResponseEntity<CautionTransactionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CautionRequest request) {
        return ResponseEntity.ok(cautionService.update(id, request));
    }

    @GetMapping("/clients/{clientId}/cautions")
    @Operation(summary = "Consulter l'historique des cautions d'un client")
    public ResponseEntity<List<CautionTransactionResponse>> getClientHistory(@PathVariable Long clientId) {
        return ResponseEntity.ok(cautionService.getClientHistory(clientId));
    }

    @GetMapping("/reservations/{id}/caution/receipt")
    @Operation(summary = "Generer le recu PDF des mouvements de caution")
    public ResponseEntity<byte[]> getReceipt(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"caution-reservation-" + id + ".pdf\"")
                .body(cautionService.generateReceipt(id));
    }
}
