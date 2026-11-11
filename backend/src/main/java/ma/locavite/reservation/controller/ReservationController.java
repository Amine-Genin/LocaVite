package ma.locavite.reservation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.locavite.reservation.dto.ExtendReservationRequest;
import ma.locavite.reservation.dto.ReservationExtensionResponse;
import ma.locavite.reservation.dto.ReservationFilter;
import ma.locavite.reservation.dto.ReservationRequest;
import ma.locavite.reservation.dto.ReservationResponse;
import ma.locavite.reservation.dto.QuoteRequest;
import ma.locavite.reservation.dto.PriceQuoteResponse;
import ma.locavite.reservation.dto.CancellationResponse;
import ma.locavite.reservation.domain.ContractLanguage;
import ma.locavite.reservation.service.ReservationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import ma.locavite.auth.domain.User;
import ma.locavite.reservation.service.ContractService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservations", description = "Endpoints de gestion et réservation de véhicules")
public class ReservationController {

    private final ReservationService reservationService;
    private final ContractService contractService;

    public ReservationController(ReservationService reservationService, ContractService contractService) {
        this.reservationService = reservationService;
        this.contractService = contractService;
    }

    @PostMapping("/quote")
    @Operation(summary = "Obtenir un devis de réservation", description = "Calcule le détail du prix sans créer de réservation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Devis calculé"),
            @ApiResponse(responseCode = "400", description = "Dates invalides ou TVA non configurée"),
            @ApiResponse(responseCode = "404", description = "Véhicule non trouvé")
    })
    public ResponseEntity<PriceQuoteResponse> quote(@Valid @RequestBody QuoteRequest request) {
        return ResponseEntity.ok(reservationService.quote(request));
    }

    @PostMapping
    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Créer une réservation (CLIENT)", description = "Crée une nouvelle réservation avec vérification des dates, de la disponibilité et des documents client.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Réservation créée avec succès"),
            @ApiResponse(responseCode = "400", description = "Dates invalides ou données manquantes"),
            @ApiResponse(responseCode = "403", description = "Documents client non validés"),
            @ApiResponse(responseCode = "404", description = "Véhicule non trouvé"),
            @ApiResponse(responseCode = "409", description = "Véhicule déjà réservé sur la période")
    })
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationRequest request) {
        ReservationResponse created = reservationService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consulter une réservation par ID", description = "Accessible par le propriétaire, l'agence propriétaire du véhicule ou un administrateur.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Détails de la réservation"),
            @ApiResponse(responseCode = "403", description = "Accès non autorisé"),
            @ApiResponse(responseCode = "404", description = "Réservation non trouvée")
    })
    public ResponseEntity<ReservationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getById(id));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Lister mes réservations (CLIENT)", description = "Retourne la liste paginée des réservations du client connecté.")
    public ResponseEntity<Page<ReservationResponse>> getMyReservations(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(reservationService.getMe(pageable));
    }

    @GetMapping
    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("hasAnyRole('AGENCE', 'ADMIN')")
    @Operation(summary = "Lister toutes les réservations (AGENCE / ADMIN)", description = "Retourne toutes les réservations pour un ADMIN, ou uniquement celles de ses véhicules pour une AGENCE.")
    public ResponseEntity<Page<ReservationResponse>> getAll(
            ReservationFilter filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(reservationService.getAll(filter, pageable));
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Modifier une réservation", description = "Modifiable par le propriétaire uniquement si la réservation est en statut PENDING.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réservation modifiée avec succès"),
            @ApiResponse(responseCode = "400", description = "Statut non modifiable ou dates invalides"),
            @ApiResponse(responseCode = "403", description = "Accès refusé"),
            @ApiResponse(responseCode = "404", description = "Réservation non trouvée"),
            @ApiResponse(responseCode = "409", description = "Véhicule non disponible sur la nouvelle période")
    })
    public ResponseEntity<ReservationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.ok(reservationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Annuler une réservation", description = "Passe le statut de la réservation à CANCELLED.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Réservation annulée avec succès"),
            @ApiResponse(responseCode = "400", description = "Réservation déjà terminée ou annulée"),
            @ApiResponse(responseCode = "403", description = "Accès refusé"),
            @ApiResponse(responseCode = "404", description = "Réservation non trouvée")
    })
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        reservationService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancel")
    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Annuler une réservation et calculer le remboursement",
            description = "Annule la réservation, libère la caution et génère un avoir PDF.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réservation annulée et remboursement calculé"),
            @ApiResponse(responseCode = "400", description = "Réservation déjà terminée ou annulée"),
            @ApiResponse(responseCode = "403", description = "Accès refusé"),
            @ApiResponse(responseCode = "404", description = "Réservation non trouvée")
    })
    public ResponseEntity<CancellationResponse> cancelWithRefund(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.cancelWithRefund(id));
    }

    @PatchMapping("/{id}/confirm")
    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("hasAnyRole('AGENCE', 'ADMIN')")
    @Operation(summary = "Confirmer une réservation", description = "Passe le statut à CONFIRMED.")
    public ResponseEntity<ReservationResponse> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.confirm(id));
    }

    @PatchMapping("/{id}/complete")
    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("hasAnyRole('AGENCE', 'ADMIN')")
    @Operation(summary = "Terminer une réservation", description = "Passe le statut à COMPLETED.")
    public ResponseEntity<ReservationResponse> complete(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.complete(id));
    }

    @PostMapping("/{id}/extend")
    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Prolonger une réservation en cours (CLIENT)", description = "Prolonge une réservation CONFIRMED ou ACTIVE après vérification de la disponibilité sur la période supplémentaire, recalcule le prix, régénère le contrat PDF et notifie l'agence.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réservation prolongée avec succès"),
            @ApiResponse(responseCode = "400", description = "Dates invalides, prix non accepté ou statut incompatible"),
            @ApiResponse(responseCode = "403", description = "Accès refusé"),
            @ApiResponse(responseCode = "404", description = "Réservation non trouvée"),
            @ApiResponse(responseCode = "409", description = "Véhicule indisponible sur la période supplémentaire")
    })
    public ResponseEntity<ReservationResponse> extend(
            @PathVariable Long id,
            @Valid @RequestBody ExtendReservationRequest request) {
        return ResponseEntity.ok(reservationService.extend(id, request));
    }

    @GetMapping("/{id}/extensions")
    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consulter l'historique des prolongations d'une réservation", description = "Accessible par le propriétaire, l'agence propriétaire du véhicule ou un administrateur.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historique des prolongations"),
            @ApiResponse(responseCode = "403", description = "Accès non autorisé"),
            @ApiResponse(responseCode = "404", description = "Réservation non trouvée")
    })
    public ResponseEntity<List<ReservationExtensionResponse>> getExtensions(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getExtensions(id));
    }

    @GetMapping("/{id}/contract")
    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Télécharger le contrat de location", description = "Redirige le propriétaire vers un lien MinIO signé, valide 24 heures.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Lien de téléchargement PDF pré-signé"),
            @ApiResponse(responseCode = "403", description = "Le contrat appartient à un autre client"),
            @ApiResponse(responseCode = "404", description = "Réservation introuvable"),
            @ApiResponse(responseCode = "400", description = "La réservation n'est pas encore confirmée")
    })
    public ResponseEntity<Void> downloadContract(
            @PathVariable Long id,
            @RequestParam(defaultValue = "FR") String lang,
            @AuthenticationPrincipal User currentUser) {
        Long currentUserId = currentUser != null
                ? currentUser.getId()
                : reservationService.getCurrentUserId();
        String url = contractService.getDownloadUrl(id, currentUserId, ContractLanguage.from(lang));
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, url)
                .build();
    }

    @GetMapping("/{id}/contract/digital")
    @Operation(summary = "Accéder à la version numérique d'un contrat via QR code")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Lien MinIO pré-signé"),
            @ApiResponse(responseCode = "403", description = "Jeton numérique invalide")
    })
    public ResponseEntity<Void> downloadDigitalContract(
            @PathVariable Long id,
            @RequestParam(defaultValue = "FR") String lang,
            @RequestParam String token) {
        String url = contractService.getDigitalDownloadUrl(id, ContractLanguage.from(lang), token);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, url)
                .build();
    }
}
