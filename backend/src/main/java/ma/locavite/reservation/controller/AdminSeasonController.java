package ma.locavite.reservation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.locavite.reservation.dto.SeasonRequest;
import ma.locavite.reservation.dto.SeasonResponse;
import ma.locavite.reservation.service.SeasonService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/seasons")
@Tag(name = "Seasons Admin", description = "Endpoints d'administration et de configuration des coefficients saisonniers")
@SecurityRequirement(name = "BearerAuth")
public class AdminSeasonController {

    private final SeasonService seasonService;

    public AdminSeasonController(SeasonService seasonService) {
        this.seasonService = seasonService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENCE')")
    @Operation(summary = "Lister toutes les saisons (ADMIN / AGENCE)", description = "Retourne l'ensemble des saisons et surcharges tarifaires configurées.")
    public ResponseEntity<List<SeasonResponse>> getAllSeasons() {
        return ResponseEntity.ok(seasonService.getAllResponses());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENCE')")
    @Operation(summary = "Créer une saison tarifaire (ADMIN / AGENCE)", description = "Permet de configurer un nouveau coefficient saisonnier global ou spécifique à une agence/véhicule.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Saison créée avec succès",
                    content = @Content(schema = @Schema(implementation = SeasonResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données invalides ou dates incohérentes", content = @Content),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès refusé", content = @Content)
    })
    public ResponseEntity<SeasonResponse> createSeason(@Valid @RequestBody SeasonRequest request) {
        SeasonResponse created = seasonService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENCE')")
    @Operation(summary = "Modifier une saison tarifaire (ADMIN / AGENCE)", description = "Met à jour les dates, coefficient ou priorité d'une saison existante et recalcule les prix des réservations futures.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saison modifiée avec succès",
                    content = @Content(schema = @Schema(implementation = SeasonResponse.class))),
            @ApiResponse(responseCode = "400", description = "Données invalides ou dates incohérentes", content = @Content),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès refusé", content = @Content),
            @ApiResponse(responseCode = "404", description = "Saison introuvable", content = @Content)
    })
    public ResponseEntity<SeasonResponse> updateSeason(
            @PathVariable Long id,
            @Valid @RequestBody SeasonRequest request) {
        SeasonResponse updated = seasonService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer une saison tarifaire (ADMIN)", description = "Supprime une saison tarifaire.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Saison supprimée avec succès"),
            @ApiResponse(responseCode = "401", description = "Non authentifié", content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès refusé", content = @Content),
            @ApiResponse(responseCode = "404", description = "Saison introuvable", content = @Content)
    })
    public ResponseEntity<Void> deleteSeason(@PathVariable Long id) {
        seasonService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
