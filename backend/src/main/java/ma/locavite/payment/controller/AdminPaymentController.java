package ma.locavite.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import ma.locavite.payment.dto.PaymentResponse;
import ma.locavite.payment.service.CashPaymentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/payments")
@Tag(name = "Admin Payments", description = "Endpoints d'administration et validation des paiements par l'agence")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasAnyRole('AGENCE', 'ADMIN')")
public class AdminPaymentController {

    private final CashPaymentService cashPaymentService;

    public AdminPaymentController(CashPaymentService cashPaymentService) {
        this.cashPaymentService = cashPaymentService;
    }

    @PutMapping("/{id}/confirm-cash")
    @Operation(
            summary = "Confirmer la réception d'un paiement en espèces (AGENCE / ADMIN)",
            description = "Passe le statut du paiement espèces de PENDING à SUCCESS, génère le reçu PDF (ticket de caisse) et notifie le client."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paiement en espèces confirmé avec succès"),
            @ApiResponse(responseCode = "400", description = "Statut invalide ou méthode non espèces"),
            @ApiResponse(responseCode = "403", description = "Accès refusé"),
            @ApiResponse(responseCode = "404", description = "Paiement ou réservation non trouvé")
    })
    public ResponseEntity<PaymentResponse> confirmCash(@PathVariable Long id) {
        return ResponseEntity.ok(cashPaymentService.confirmCash(id));
    }

    @GetMapping("/{id}/receipt")
    @Operation(
            summary = "Télécharger le reçu PDF (ticket de caisse) d'un paiement en espèces (AGENCE / ADMIN)",
            description = "Génère et retourne le ticket de caisse PDF d'un paiement en espèces confirmé."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reçu PDF généré"),
            @ApiResponse(responseCode = "400", description = "Paiement non confirmé"),
            @ApiResponse(responseCode = "403", description = "Accès refusé"),
            @ApiResponse(responseCode = "404", description = "Paiement non trouvé")
    })
    public ResponseEntity<byte[]> getReceipt(@PathVariable Long id) {
        byte[] pdf = cashPaymentService.generateReceipt(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ticket-caisse-" + id + ".pdf\"")
                .body(pdf);
    }
}
