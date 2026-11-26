package ma.locavite.admin.dashboard;

import ma.locavite.auth.domain.User;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'AGENCE')")
public class DashboardController {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Africa/Casablanca");
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {
        LocalDate end = to == null ? LocalDate.now(BUSINESS_ZONE) : to;
        LocalDate start = from == null ? end.minusDays(29) : from;
        if (start.isAfter(end) || start.isBefore(end.minusYears(2))) {
            return ResponseEntity.badRequest().build();
        }

        Long agencyId = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))
                ? null
                : ((User) authentication.getPrincipal()).getId();
        return ResponseEntity.ok(dashboardService.getDashboard(start, end, agencyId));
    }
}
