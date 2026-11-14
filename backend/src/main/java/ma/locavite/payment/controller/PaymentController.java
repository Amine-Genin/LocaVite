package ma.locavite.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.locavite.payment.dto.PaymentFilter;
import ma.locavite.payment.dto.PaymentRequest;
import ma.locavite.payment.dto.PaymentResponse;
import ma.locavite.payment.service.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Endpoints de gestion et suivi des paiements")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Initier un paiement (CLIENT)", description = "Crée un nouveau paiement pour une réservation avec le statut PENDING.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Paiement initié avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides ou montant supérieur au restant dû"),
            @ApiResponse(responseCode = "403", description = "Accès refusé"),
            @ApiResponse(responseCode = "404", description = "Réservation non trouvée")
    })
    public ResponseEntity<PaymentResponse> initiate(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.initiate(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consulter un paiement par ID", description = "Accessible par le propriétaire ou un administrateur.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Détails du paiement"),
            @ApiResponse(responseCode = "403", description = "Accès non autorisé"),
            @ApiResponse(responseCode = "404", description = "Paiement non trouvé")
    })
    public ResponseEntity<PaymentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getById(id));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Lister mes paiements (CLIENT)", description = "Retourne la liste paginée des paiements du client connecté.")
    public ResponseEntity<Page<PaymentResponse>> getMe(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(paymentService.getMe(pageable));
    }

    @GetMapping
    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("hasAnyRole('AGENCE', 'ADMIN')")
    @Operation(summary = "Lister les paiements (AGENCE / ADMIN)", description = "Retourne tous les paiements pour un ADMIN ou ceux des véhicules d'une AGENCE.")
    public ResponseEntity<Page<PaymentResponse>> getAll(
            PaymentFilter filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(paymentService.getAll(filter, pageable));
    }

    @PostMapping("/{id}/cancel")
    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Annuler un paiement", description = "Annule un paiement si son statut permet la transition vers CANCELLED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paiement annulé avec succès"),
            @ApiResponse(responseCode = "400", description = "Statut incompatible avec l'annulation"),
            @ApiResponse(responseCode = "403", description = "Accès refusé"),
            @ApiResponse(responseCode = "404", description = "Paiement non trouvé")
    })
    public ResponseEntity<PaymentResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.cancel(id));
    }

    @PostMapping("/{id}/confirm")
    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Confirmer un paiement (ADMIN)", description = "Confirme un paiement et passe son statut à SUCCESS.")
    public ResponseEntity<PaymentResponse> confirm(
            @PathVariable Long id,
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) String metadata) {
        return ResponseEntity.ok(paymentService.confirm(id, transactionId, reference, metadata));
    }

    @PostMapping("/{id}/fail")
    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Marquer un paiement en échec (ADMIN)", description = "Passe le statut du paiement à FAILED.")
    public ResponseEntity<PaymentResponse> fail(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(paymentService.fail(id, reason));
    }
}
