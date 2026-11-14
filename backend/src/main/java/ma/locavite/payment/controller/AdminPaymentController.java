package ma.locavite.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.locavite.payment.dto.PaymentResponse;
import ma.locavite.payment.dto.RefundRequest;
import ma.locavite.payment.dto.RefundResponse;
import ma.locavite.payment.service.CashPaymentService;
import ma.locavite.payment.service.RefundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/payments")
@Tag(name = "Admin Payments", description = "Endpoints d'administration, validation et remboursement des paiements par l'agence")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasAnyRole('AGENCE', 'ADMIN')")
public class AdminPaymentController {

    private final CashPaymentService cashPaymentService;
    private final RefundService refundService;

    public AdminPaymentController(CashPaymentService cashPaymentService) {
        this(cashPaymentService, null);
    }

    @Autowired
    public AdminPaymentController(CashPaymentService cashPaymentService, RefundService refundService) {
        this.cashPaymentService = cashPaymentService;
        this.refundService = refundService;
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

    @PostMapping("/{id}/refund")
    @Operation(
            summary = "Rembourser un client totalement ou partiellement (AGENCE / ADMIN)",
            description = "Effectue un remboursement total ou partiel (CMI via API dédiée ou manuel par virement), met à jour le statut du paiement (PARTIALLY_REFUNDED ou REFUNDED), génère un avoir PDF et notifie le client (email + SMS)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Remboursement effectué avec succès"),
            @ApiResponse(responseCode = "400", description = "Montant supérieur au solde remboursable ou statut invalide"),
            @ApiResponse(responseCode = "403", description = "Accès refusé"),
            @ApiResponse(responseCode = "404", description = "Paiement non trouvé")
    })
    public ResponseEntity<RefundResponse> refund(
            @PathVariable Long id,
            @Valid @RequestBody RefundRequest request) {
        return ResponseEntity.ok(refundService.create(id, request));
    }

    @GetMapping("/{id}/refunds")
    @Operation(
            summary = "Lister l'historique des remboursements d'un paiement (AGENCE / ADMIN)",
            description = "Retourne la liste des remboursements associés à un paiement avec leur piste d'audit."
    )
    public ResponseEntity<List<RefundResponse>> getRefunds(@PathVariable Long id) {
        return ResponseEntity.ok(refundService.getByPaymentId(id));
    }

    @GetMapping("/{id}/avoir")
    @Operation(
            summary = "Télécharger l'avoir PDF de remboursement d'un paiement (AGENCE / ADMIN)",
            description = "Retourne l'avoir PDF généré lors du remboursement du paiement."
    )
    public ResponseEntity<byte[]> getAvoir(@PathVariable Long id) {
        byte[] pdf = refundService.downloadAvoirPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"avoir-paiement-" + id + ".pdf\"")
                .body(pdf);
    }
}
