package ma.locavite.sale.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import ma.locavite.sale.dto.VisitConfirmBody;
import ma.locavite.sale.dto.VisitRequestBody;
import ma.locavite.sale.dto.VisitRequestResponse;
import ma.locavite.sale.dto.VisitRescheduleBody;
import ma.locavite.sale.service.VisitService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class VisitRequestController {

    private final VisitService visitService;

    public VisitRequestController(VisitService visitService) {
        this.visitService = visitService;
    }

    @PostMapping("/api/sale-listings/{id}/visit-request")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Demander une visite ou un essai")
    public ResponseEntity<VisitRequestResponse> create(
            @PathVariable Long id,
            @Valid @RequestBody VisitRequestBody body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(visitService.create(id, body));
    }

    @GetMapping("/api/admin/visit-requests")
    @PreAuthorize("hasAnyRole('AGENCE', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    public List<VisitRequestResponse> listForAgency() {
        return visitService.listForAgency();
    }

    @PutMapping("/api/admin/visit-requests/{id}/confirm")
    @PreAuthorize("hasAnyRole('AGENCE', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    public VisitRequestResponse confirm(
            @PathVariable Long id,
            @RequestBody(required = false) @Valid VisitConfirmBody body) {
        return visitService.confirm(id, body);
    }

    @PutMapping("/api/admin/visit-requests/{id}/reschedule")
    @PreAuthorize("hasAnyRole('AGENCE', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    public VisitRequestResponse reschedule(
            @PathVariable Long id,
            @Valid @RequestBody VisitRescheduleBody body) {
        return visitService.reschedule(id, body);
    }

    @PutMapping("/api/visit-requests/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    public VisitRequestResponse cancelByClient(@PathVariable Long id) {
        return visitService.cancelByClient(id);
    }
}
