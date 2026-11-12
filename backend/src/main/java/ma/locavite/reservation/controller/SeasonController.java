package ma.locavite.reservation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import ma.locavite.reservation.dto.SeasonResponse;
import ma.locavite.reservation.service.SeasonService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seasons")
@Tag(name = "Seasons", description = "Consultation publique des tarifs et saisons de location")
public class SeasonController {

    private final SeasonService seasonService;

    public SeasonController(SeasonService seasonService) {
        this.seasonService = seasonService;
    }

    @GetMapping
    @Operation(summary = "Lister les saisons tarifaires", description = "Retourne la liste des saisons tarifaires (publiques et overrides optionnels).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des saisons récupérée",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SeasonResponse.class))))
    })
    public ResponseEntity<List<SeasonResponse>> getSeasons(
            @Parameter(description = "Identifiant de l'agence (optionnel)") @RequestParam(required = false) Long agenceId,
            @Parameter(description = "Identifiant du véhicule (optionnel)") @RequestParam(required = false) Long vehicleId) {
        if (agenceId != null || vehicleId != null) {
            return ResponseEntity.ok(seasonService.getApplicable(agenceId, vehicleId));
        }
        return ResponseEntity.ok(seasonService.getAllResponses());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter une saison tarifaire par son identifiant", description = "Retourne le détail d'une saison spécifique.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saison trouvée",
                    content = @Content(schema = @Schema(implementation = SeasonResponse.class))),
            @ApiResponse(responseCode = "404", description = "Saison non trouvée", content = @Content)
    })
    public ResponseEntity<SeasonResponse> getSeasonById(@PathVariable Long id) {
        return ResponseEntity.ok(seasonService.getResponseById(id));
    }
}
