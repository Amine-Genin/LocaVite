package ma.locavite.reservation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.locavite.reservation.dto.*;
import ma.locavite.reservation.service.PricingAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Pricing Admin", description = "Administration des options tarifaires, remises et simulation de prix")
@SecurityRequirement(name = "BearerAuth")
public class AdminPricingController {

    private final PricingAdminService pricingAdminService;

    public AdminPricingController(PricingAdminService pricingAdminService) {
        this.pricingAdminService = pricingAdminService;
    }

    // ==================== OPTIONS ====================

    @GetMapping("/options")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENCE')")
    @Operation(summary = "Lister toutes les options tarifaires")
    public ResponseEntity<List<OptionPriceResponse>> getAllOptions() {
        return ResponseEntity.ok(pricingAdminService.getAllOptions());
    }

    @PostMapping("/options")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer une option tarifaire")
    public ResponseEntity<OptionPriceResponse> createOption(@Valid @RequestBody OptionPriceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pricingAdminService.createOption(request));
    }

    @PutMapping("/options/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier une option tarifaire")
    public ResponseEntity<OptionPriceResponse> updateOption(
            @PathVariable Long id,
            @Valid @RequestBody OptionPriceRequest request) {
        return ResponseEntity.ok(pricingAdminService.updateOption(id, request));
    }

    @DeleteMapping("/options/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer une option tarifaire")
    public ResponseEntity<Void> deleteOption(@PathVariable Long id) {
        pricingAdminService.deleteOption(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== DISCOUNTS ====================

    @GetMapping("/discounts")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENCE')")
    @Operation(summary = "Lister toutes les remises (longue durée et codes promo)")
    public ResponseEntity<List<DiscountResponse>> getAllDiscounts() {
        return ResponseEntity.ok(pricingAdminService.getAllDiscounts());
    }

    @PostMapping("/discounts")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer une remise longue durée ou un code promo")
    public ResponseEntity<DiscountResponse> createDiscount(@Valid @RequestBody DiscountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pricingAdminService.createDiscount(request));
    }

    @PutMapping("/discounts/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier une remise existante")
    public ResponseEntity<DiscountResponse> updateDiscount(
            @PathVariable Long id,
            @Valid @RequestBody DiscountRequest request) {
        return ResponseEntity.ok(pricingAdminService.updateDiscount(id, request));
    }

    @DeleteMapping("/discounts/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer une remise")
    public ResponseEntity<Void> deleteDiscount(@PathVariable Long id) {
        pricingAdminService.deleteDiscount(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== SIMULATION ====================

    @PostMapping("/pricing/simulate")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENCE')")
    @Operation(summary = "Simuler le calcul complet d'un prix (saisons, options, remises, code promo, TVA, caution)")
    public ResponseEntity<PricingSimulateResponse> simulatePrice(
            @Valid @RequestBody PricingSimulateRequest request) {
        return ResponseEntity.ok(pricingAdminService.simulatePrice(request));
    }
}
