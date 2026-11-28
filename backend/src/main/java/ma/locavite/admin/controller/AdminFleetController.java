package ma.locavite.admin.controller;

import jakarta.validation.Valid;
import ma.locavite.vehicle.domain.VehicleStatus;
import ma.locavite.vehicle.dto.*;
import ma.locavite.vehicle.service.FleetService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('AGENCE', 'ADMIN')")
public class AdminFleetController {
    private final FleetService fleetService;
    public AdminFleetController(FleetService fleetService) { this.fleetService = fleetService; }

    @GetMapping("/fleet")
    public List<FleetVehicleResponse> getFleet(@RequestParam(required = false) VehicleStatus statut,
            @RequestParam(required = false) String categorie, @RequestParam(required = false) String ville) {
        return fleetService.getFleet(statut, categorie, ville);
    }

    @PutMapping("/vehicles/{id}/status")
    public FleetVehicleResponse updateStatus(@PathVariable Long id, @Valid @RequestBody VehicleStatusRequest request) {
        return fleetService.changeStatus(id, request);
    }

    @GetMapping("/vehicles/{id}/history")
    public List<FleetHistoryResponse> getHistory(@PathVariable Long id) { return fleetService.getHistory(id); }

    @PutMapping("/vehicles/{id}/agency")
    @PreAuthorize("hasRole('ADMIN')")
    public FleetVehicleResponse transfer(@PathVariable Long id, @Valid @RequestBody VehicleTransferRequest request) {
        return fleetService.transfer(id, request);
    }

    @PostMapping("/vehicles/{id}/maintenance")
    public FleetHistoryResponse addMaintenance(@PathVariable Long id, @Valid @RequestBody VehicleMaintenanceRequest request) {
        return fleetService.addMaintenance(id, request);
    }

    @PostMapping("/vehicles/{id}/incidents")
    public FleetHistoryResponse addIncident(@PathVariable Long id, @Valid @RequestBody VehicleIncidentRequest request) {
        return fleetService.addIncident(id, request);
    }
}
