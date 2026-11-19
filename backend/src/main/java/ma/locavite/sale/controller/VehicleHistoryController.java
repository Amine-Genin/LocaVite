package ma.locavite.sale.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import ma.locavite.sale.dto.CreateVehicleHistoryRequest;
import ma.locavite.sale.dto.VehicleHistoryResponse;
import ma.locavite.sale.service.VehicleHistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class VehicleHistoryController {

    private final VehicleHistoryService service;

    public VehicleHistoryController(VehicleHistoryService service) {
        this.service = service;
    }

    @GetMapping("/sale-listings/{id}/history")
    @Operation(summary = "Consulter l'historique d'une annonce publiée")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historique chronologique récupéré",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = VehicleHistoryResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Annonce introuvable ou non publiée")
    })
    public ResponseEntity<List<VehicleHistoryResponse>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(service.getHistory(id));
    }

    @PostMapping(value = "/admin/sale-listings/{id}/history", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('AGENCE', 'ADMIN')")
    @Operation(summary = "Ajouter un événement à l'historique d'une annonce")
    @SecurityRequirement(name = "BearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Événement ajouté",
                    content = @Content(schema = @Schema(implementation = VehicleHistoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données ou document invalides"),
            @ApiResponse(responseCode = "403", description = "Agence non propriétaire ou rôle insuffisant"),
            @ApiResponse(responseCode = "404", description = "Annonce introuvable"),
            @ApiResponse(responseCode = "500", description = "Erreur de stockage du justificatif")
    })
    public ResponseEntity<VehicleHistoryResponse> addEvent(
            @PathVariable Long id,
            @Valid @RequestPart("request") CreateVehicleHistoryRequest request,
            @RequestPart(value = "document", required = false) MultipartFile document) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addEvent(id, request, document));
    }
}
