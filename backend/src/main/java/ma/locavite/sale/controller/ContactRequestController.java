package ma.locavite.sale.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import ma.locavite.sale.dto.ContactReplyBody;
import ma.locavite.sale.dto.ContactRequestBody;
import ma.locavite.sale.dto.ContactRequestResponse;
import ma.locavite.sale.service.ContactRateLimiter;
import ma.locavite.sale.service.ContactRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class ContactRequestController {

    private final ContactRequestService service;
    private final ContactRateLimiter rateLimiter;

    public ContactRequestController(ContactRequestService service, ContactRateLimiter rateLimiter) {
        this.service = service;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/api/sale-listings/{id}/contact")
    @Operation(summary = "Envoyer une demande de contact pour une annonce publiée")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Demande enregistrée",
                    content = @Content(schema = @Schema(implementation = ContactRequestResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données de contact invalides"),
            @ApiResponse(responseCode = "404", description = "Annonce publiée introuvable"),
            @ApiResponse(responseCode = "429", description = "Limite de demandes par adresse IP atteinte")
    })
    public ResponseEntity<ContactRequestResponse> create(
            @PathVariable Long id,
            @Valid @RequestBody ContactRequestBody body,
            HttpServletRequest request) {
        if (!rateLimiter.tryConsume(request.getRemoteAddr())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Limite de demandes atteinte");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(id, body));
    }

    @GetMapping("/api/admin/contact-requests")
    @PreAuthorize("hasAnyRole('AGENCE', 'ADMIN')")
    @Operation(summary = "Lister les demandes de contact de l'agence")
    @ApiResponse(responseCode = "200", description = "Demandes de contact",
            content = @Content(array = @ArraySchema(
                    schema = @Schema(implementation = ContactRequestResponse.class))))
    public List<ContactRequestResponse> listForAgency() {
        return service.listForAgency();
    }

    @PutMapping("/api/admin/contact-requests/{id}/reply")
    @PreAuthorize("hasAnyRole('AGENCE', 'ADMIN')")
    @Operation(summary = "Répondre à une demande de contact")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réponse enregistrée",
                    content = @Content(schema = @Schema(implementation = ContactRequestResponse.class))),
            @ApiResponse(responseCode = "400", description = "Réponse invalide"),
            @ApiResponse(responseCode = "403", description = "Demande appartenant à une autre agence"),
            @ApiResponse(responseCode = "404", description = "Demande introuvable")
    })
    public ContactRequestResponse reply(
            @PathVariable Long id,
            @Valid @RequestBody ContactReplyBody body) {
        return service.reply(id, body);
    }
}
