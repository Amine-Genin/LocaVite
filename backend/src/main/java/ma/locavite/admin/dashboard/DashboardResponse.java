package ma.locavite.admin.dashboard;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
        Kpis kpis,
        Charts charts,
        Recent recent
) {
    public record Kpis(
            BigDecimal monthlyRevenue,
            BigDecimal previousMonthRevenue,
            long activeReservations,
            long availableVehicles,
            long totalVehicles,
            double occupancyRate,
            long clients,
            long salesInProgress
    ) {}

    public record Charts(
            List<RevenuePoint> revenue,
            List<CountPoint> reservationsByStatus,
            List<CountPoint> vehiclesByCategory,
            List<CountPoint> topVehicles,
            List<RatePoint> occupancyByMonth
    ) {}

    public record Recent(
            List<ReservationRow> reservations,
            List<PaymentRow> payments,
            List<ContactRow> contacts
    ) {}

    public record RevenuePoint(String label, BigDecimal amount) {}
    public record CountPoint(String label, long count) {}
    public record RatePoint(String label, double rate) {}
    public record ReservationRow(Long id, String code, String client, String vehicle,
                                 String status, LocalDate startDate, LocalDate endDate,
                                 BigDecimal amount, Instant createdAt) {}
    public record PaymentRow(Long id, String code, String client, BigDecimal amount,
                             String status, Instant createdAt) {}
    public record ContactRow(Long id, Long saleListingId, String name, String email,
                             String message, String status, Instant createdAt) {}
}
