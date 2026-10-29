package ma.locavite.vehicle.controller;

import jakarta.validation.Valid;
import ma.locavite.vehicle.dto.AvailabilityResponse;
import ma.locavite.vehicle.dto.VehicleFilter;
import ma.locavite.vehicle.dto.VehicleRequest;
import ma.locavite.vehicle.dto.VehicleResponse;
import ma.locavite.vehicle.service.AvailabilityService;
import ma.locavite.vehicle.service.VehicleService;
import ma.locavite.vehicle.service.VehicleSearchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;
    private final VehicleSearchService vehicleSearchService;
    private final AvailabilityService availabilityService;

    public VehicleController(
            VehicleService vehicleService,
            VehicleSearchService vehicleSearchService,
            AvailabilityService availabilityService) {
        this.vehicleService = vehicleService;
        this.vehicleSearchService = vehicleSearchService;
        this.availabilityService = availabilityService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('AGENCE', 'ADMIN')")
    public ResponseEntity<VehicleResponse> create(@Valid @RequestBody VehicleRequest request) {
        VehicleResponse created = vehicleService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public VehicleResponse getById(@PathVariable Long id) {
        return vehicleService.getById(id);
    }

    @GetMapping
    public PagedModel<VehicleResponse> search(
            VehicleFilter filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(vehicleService.search(filter, pageable));
    }

    @GetMapping("/search")
    public Page<VehicleResponse> searchVehicles(
            @RequestParam String ville,
            VehicleFilter filter,
            @PageableDefault(size = 20, sort = "prixJour", direction = Sort.Direction.ASC) Pageable pageable) {
        return vehicleSearchService.search(filter.withVille(ville), pageable);
    }

    @GetMapping("/{id}/availability")
    public AvailabilityResponse getAvailability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return availabilityService.getAvailability(id, from, to);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENCE', 'ADMIN')")
    public VehicleResponse update(@PathVariable Long id, @Valid @RequestBody VehicleRequest request) {
        return vehicleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENCE', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vehicleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
