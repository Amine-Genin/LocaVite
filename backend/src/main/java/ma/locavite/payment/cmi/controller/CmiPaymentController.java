package ma.locavite.payment.cmi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import ma.locavite.payment.cmi.dto.CmiInitiateRequest;
import ma.locavite.payment.cmi.dto.CmiInitiateResponse;
import ma.locavite.payment.cmi.dto.CmiWebhookResponse;
import ma.locavite.payment.cmi.service.CmiPaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/cmi")
@Tag(name = "Paiement CMI", description = "Endpoints d'intégration de la passerelle de paiement en ligne CMI (Centre Monétique Interbancaire)")
public class CmiPaymentController {

    private final CmiPaymentService cmiPaymentService;

    public CmiPaymentController(CmiPaymentService cmiPaymentService) {
        this.cmiPaymentService = cmiPaymentService;
    }

    @PostMapping("/initiate")
    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Initier un paiement en ligne via CMI", description = "Génère une session de paiement CMI avec signature HMAC-SHA512 et retourne l'URL de redirection et les paramètres.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session CMI initialisée avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides ou dépassement du montant restant dû"),
            @ApiResponse(responseCode = "403", description = "Accès refusé"),
            @ApiResponse(responseCode = "404", description = "Réservation non trouvée"),
            @ApiResponse(responseCode = "503", description = "Passerelle CMI indisponible (mode fallback)")
    })
    public ResponseEntity<CmiInitiateResponse> initiate(
            @Valid @RequestBody CmiInitiateRequest request,
            HttpServletRequest httpRequest) {
        CmiInitiateResponse response = cmiPaymentService.initiate(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/callback", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.ALL_VALUE})
    @Operation(summary = "Callback CMI après paiement", description = "Appelé par CMI après passage par la passerelle 3D-Secure. Vérifie la signature et redirige vers la confirmation.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Paiement traité et redirection vers la page de confirmation"),
            @ApiResponse(responseCode = "400", description = "Signature ou hash CMI invalide"),
            @ApiResponse(responseCode = "404", description = "Paiement non trouvé")
    })
    public ResponseEntity<Void> callbackPost(
            @RequestParam(required = false) Map<String, String> requestParams,
            @RequestBody(required = false) Map<String, String> bodyParams,
            HttpServletRequest httpRequest) {
        Map<String, String> merged = mergeParams(requestParams, bodyParams);
        String redirectUrl = cmiPaymentService.handleCallback(merged, httpRequest);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
    }

    @GetMapping("/callback")
    @Operation(summary = "Callback CMI GET (redirection navigateur)", description = "Permet de traiter la redirection GET retour de la passerelle CMI si applicable.")
    public ResponseEntity<Void> callbackGet(
            @RequestParam Map<String, String> requestParams,
            HttpServletRequest httpRequest) {
        String redirectUrl = cmiPaymentService.handleCallback(requestParams, httpRequest);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
    }

    @PostMapping(value = "/webhook", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.ALL_VALUE})
    @Operation(summary = "Notification serveur-à-serveur CMI", description = "Notification directe CMI. Vérifie l'authenticité (IP whitelist + signature) et garantit l'idempotence.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification prise en compte (ACTION=POSTAUTH)"),
            @ApiResponse(responseCode = "400", description = "Signature CMI invalide"),
            @ApiResponse(responseCode = "403", description = "IP appelante non autorisée"),
            @ApiResponse(responseCode = "404", description = "Paiement non trouvé")
    })
    public ResponseEntity<String> webhook(
            @RequestParam(required = false) Map<String, String> requestParams,
            @RequestBody(required = false) Map<String, String> bodyParams,
            HttpServletRequest httpRequest) {
        Map<String, String> merged = mergeParams(requestParams, bodyParams);
        CmiWebhookResponse response = cmiPaymentService.handleWebhook(merged, httpRequest);
        return ResponseEntity.ok(response.action());
    }

    private Map<String, String> mergeParams(Map<String, String> requestParams, Map<String, String> bodyParams) {
        Map<String, String> merged = new HashMap<>();
        if (requestParams != null) {
            merged.putAll(requestParams);
        }
        if (bodyParams != null) {
            merged.putAll(bodyParams);
        }
        return merged;
    }
}
