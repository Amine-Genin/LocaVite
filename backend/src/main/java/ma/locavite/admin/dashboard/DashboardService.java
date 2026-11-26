package ma.locavite.admin.dashboard;

import ma.locavite.admin.dashboard.DashboardResponse.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Africa/Casablanca");
    private final JdbcTemplate jdbc;

    public DashboardService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Cacheable(cacheNames = "adminDashboard", key = "#from + ':' + #to + ':' + #agencyId")
    public DashboardResponse getDashboard(LocalDate from, LocalDate to, Long agencyId) {
        String vehicleScope = agencyId == null ? "1 = 1" : "v.agence_id = ?";
        String reservationScope = agencyId == null ? "1 = 1" : "EXISTS (SELECT 1 FROM vehicles v WHERE v.id = r.vehicle_id AND v.agence_id = ?)";
        String paymentScope = agencyId == null ? "1 = 1" : "EXISTS (SELECT 1 FROM reservations r JOIN vehicles v ON v.id = r.vehicle_id WHERE r.id = p.reservation_id AND v.agence_id = ?)";
        String saleScope = agencyId == null ? "1 = 1" : "s.agence_id = ?";
        String contactScope = agencyId == null ? "1 = 1" : "EXISTS (SELECT 1 FROM sale_listings s WHERE s.id = c.sale_listing_id AND s.agence_id = ?)";

        Object[] scopeArgs = agencyId == null ? new Object[0] : new Object[]{agencyId};
        long totalVehicles = count("SELECT COUNT(*) FROM vehicles v WHERE " + vehicleScope, scopeArgs);
        long availableVehicles = count("SELECT COUNT(*) FROM vehicles v WHERE v.statut = 'PUBLISHED' AND " + vehicleScope, scopeArgs);
        long occupancyFleet = count("SELECT COUNT(*) FROM vehicles v WHERE v.statut IN ('PUBLISHED', 'RENTED') AND " + vehicleScope, scopeArgs);
        long activeReservations = count("SELECT COUNT(*) FROM reservations r WHERE r.statut = 'ACTIVE' AND " + reservationScope, scopeArgs);
        long clients = agencyId == null
                ? jdbc.queryForObject("SELECT COUNT(DISTINCT ur.user_id) FROM user_roles ur JOIN roles ro ON ro.id = ur.role_id WHERE ro.name = 'CLIENT'", Long.class)
                : count("SELECT COUNT(DISTINCT r.user_id) FROM reservations r WHERE " + reservationScope, scopeArgs);
        long salesInProgress = count("SELECT COUNT(*) FROM sale_listings s WHERE s.deleted = FALSE AND s.statut IN ('PUBLISHED', 'RESERVED') AND " + saleScope, scopeArgs);

        YearMonth currentMonth = YearMonth.now(BUSINESS_ZONE);
        BigDecimal monthlyRevenue = revenueForMonth(currentMonth, agencyId, paymentScope);
        BigDecimal previousMonthRevenue = revenueForMonth(currentMonth.minusMonths(1), agencyId, paymentScope);

        double occupancyRate = occupancy(from, to, occupancyFleet, agencyId);

        Charts charts = new Charts(
                revenue(from, to, paymentScope, scopeArgs),
                countRows("SELECT r.statut AS label, COUNT(*) AS count FROM reservations r WHERE r.created_at >= ? AND r.created_at < ? AND " + reservationScope + " GROUP BY r.statut ORDER BY count DESC",
                        prepend(rangeArgs(from, to), scopeArgs)),
                countRows("SELECT c.name AS label, COUNT(*) AS count FROM vehicles v JOIN categories c ON c.id = v.category_id WHERE " + vehicleScope + " GROUP BY c.name ORDER BY count DESC",
                        scopeArgs),
                topVehicles(from, to, reservationScope, scopeArgs),
                monthlyOccupancy(from, to, occupancyFleet, agencyId)
        );

        Recent recent = new Recent(
                recentReservations(reservationScope, scopeArgs),
                recentPayments(paymentScope, scopeArgs),
                recentContacts(contactScope, scopeArgs)
        );

        return new DashboardResponse(
                new Kpis(monthlyRevenue, previousMonthRevenue, activeReservations,
                        availableVehicles, totalVehicles, occupancyRate, clients, salesInProgress),
                charts,
                recent
        );
    }

    private BigDecimal revenueForMonth(YearMonth month, Long agencyId, String paymentScope) {
        Instant start = month.atDay(1).atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant end = month.plusMonths(1).atDay(1).atStartOfDay(BUSINESS_ZONE).toInstant();
        Object[] args = agencyId == null
                ? new Object[]{Timestamp.from(start), Timestamp.from(end)}
                : new Object[]{Timestamp.from(start), Timestamp.from(end), agencyId};
        BigDecimal value = jdbc.queryForObject(
                "SELECT COALESCE(SUM(p.montant), 0) FROM payments p WHERE p.statut = 'SUCCESS' AND COALESCE(p.paid_at, p.created_at) >= ? AND COALESCE(p.paid_at, p.created_at) < ? AND " + paymentScope,
                BigDecimal.class, args);
        return value == null ? BigDecimal.ZERO : value;
    }

    private List<RevenuePoint> revenue(LocalDate from, LocalDate to, String paymentScope, Object[] scopeArgs) {
        String bucket = to.toEpochDay() - from.toEpochDay() <= 14 ? "day"
                : to.toEpochDay() - from.toEpochDay() <= 100 ? "week" : "month";
        Object[] args = prepend(rangeArgs(from, to), scopeArgs);
        return jdbc.query(
                "SELECT to_char(date_trunc('" + bucket + "', COALESCE(p.paid_at, p.created_at)), 'YYYY-MM-DD') AS label, COALESCE(SUM(p.montant), 0) AS amount " +
                        "FROM payments p WHERE p.statut = 'SUCCESS' AND COALESCE(p.paid_at, p.created_at) >= ? AND COALESCE(p.paid_at, p.created_at) < ? AND " + paymentScope +
                        " GROUP BY date_trunc('" + bucket + "', COALESCE(p.paid_at, p.created_at)) ORDER BY date_trunc('" + bucket + "', COALESCE(p.paid_at, p.created_at))",
                (rs, row) -> new RevenuePoint(rs.getString("label"), rs.getBigDecimal("amount")), args);
    }

    private List<CountPoint> countRows(String sql, Object[] args) {
        return jdbc.query(sql, (rs, row) -> new CountPoint(rs.getString("label"), rs.getLong("count")), args);
    }

    private List<CountPoint> topVehicles(LocalDate from, LocalDate to, String reservationScope, Object[] scopeArgs) {
        return jdbc.query(
                "SELECT CONCAT(b.name, ' ', v.modele) AS label, COUNT(r.id) AS count FROM reservations r JOIN vehicles v ON v.id = r.vehicle_id JOIN brands b ON b.id = v.brand_id " +
                        "WHERE r.created_at >= ? AND r.created_at < ? AND r.statut <> 'CANCELLED' AND " + reservationScope +
                        " GROUP BY v.id, b.name, v.modele ORDER BY count DESC LIMIT 5",
                (rs, row) -> new CountPoint(rs.getString("label"), rs.getLong("count")), prepend(rangeArgs(from, to), scopeArgs));
    }

    private double occupancy(LocalDate from, LocalDate to, long totalVehicles, Long agencyId) {
        if (totalVehicles == 0) return 0;
        String scope = agencyId == null ? "1 = 1" : "v.agence_id = ?";
        LocalDate endExclusive = to.plusDays(1);
        Double bookedDays = jdbc.queryForObject(
                "SELECT COALESCE(SUM(GREATEST(0, LEAST(r.date_fin, ?) - GREATEST(r.date_debut, ?))), 0)::double precision " +
                        "FROM reservations r JOIN vehicles v ON v.id = r.vehicle_id WHERE r.statut IN ('CONFIRMED', 'ACTIVE', 'COMPLETED') " +
                        "AND r.date_debut < ? AND r.date_fin > ? AND " + scope,
                agencyId == null
                        ? new Object[]{endExclusive, from, endExclusive, from}
                        : new Object[]{endExclusive, from, endExclusive, from, agencyId}, Double.class);
        long periodDays = Math.max(1, endExclusive.toEpochDay() - from.toEpochDay());
        return Math.min(100, (bookedDays == null ? 0 : bookedDays) / (totalVehicles * periodDays) * 100);
    }

    private List<RatePoint> monthlyOccupancy(LocalDate from, LocalDate to, long totalVehicles, Long agencyId) {
        if (totalVehicles == 0) return List.of();
        String reservationScope = agencyId == null ? "1 = 1"
                : "EXISTS (SELECT 1 FROM vehicles scoped_vehicle WHERE scoped_vehicle.id = r.vehicle_id AND scoped_vehicle.agence_id = ?)";
        return jdbc.query(
                "SELECT to_char(m.month_start, 'YYYY-MM') AS label, " +
                        "LEAST(100, COALESCE(SUM(GREATEST(0, LEAST(r.date_fin, (m.month_start + INTERVAL '1 month')::date) - GREATEST(r.date_debut, m.month_start::date))), 0)::double precision / " +
                        "(? * EXTRACT(DAY FROM (m.month_start + INTERVAL '1 month' - INTERVAL '1 day'))) * 100) AS rate " +
                        "FROM generate_series(date_trunc('month', ?::date), date_trunc('month', ?::date), INTERVAL '1 month') m(month_start) " +
                        "LEFT JOIN reservations r ON r.date_debut < (m.month_start + INTERVAL '1 month')::date AND r.date_fin > m.month_start::date AND r.statut IN ('CONFIRMED', 'ACTIVE', 'COMPLETED') AND " + reservationScope +
                        " GROUP BY m.month_start ORDER BY m.month_start",
                (rs, row) -> new RatePoint(rs.getString("label"), rs.getDouble("rate")),
                agencyId == null ? new Object[]{totalVehicles, from, to} : new Object[]{totalVehicles, from, to, agencyId});
    }

    private List<ReservationRow> recentReservations(String scope, Object[] scopeArgs) {
        return jdbc.query(
                "SELECT r.id, r.code, CONCAT(u.prenom, ' ', u.nom) AS client, CONCAT(b.name, ' ', v.modele) AS vehicle, r.statut, r.date_debut, r.date_fin, r.prix_total, r.created_at " +
                        "FROM reservations r JOIN users u ON u.id = r.user_id JOIN vehicles v ON v.id = r.vehicle_id JOIN brands b ON b.id = v.brand_id " +
                        "WHERE " + scope + " ORDER BY r.created_at DESC LIMIT 5",
                (rs, row) -> new ReservationRow(rs.getLong("id"), rs.getString("code"), rs.getString("client"),
                        rs.getString("vehicle"), rs.getString("statut"), rs.getDate("date_debut").toLocalDate(),
                        rs.getDate("date_fin").toLocalDate(), rs.getBigDecimal("prix_total"), rs.getTimestamp("created_at").toInstant()), scopeArgs);
    }

    private List<PaymentRow> recentPayments(String scope, Object[] scopeArgs) {
        return jdbc.query(
                "SELECT p.id, p.code, CONCAT(u.prenom, ' ', u.nom) AS client, p.montant, p.statut, p.created_at FROM payments p JOIN users u ON u.id = p.user_id " +
                        "WHERE " + scope + " ORDER BY p.created_at DESC LIMIT 5",
                (rs, row) -> new PaymentRow(rs.getLong("id"), rs.getString("code"), rs.getString("client"),
                        rs.getBigDecimal("montant"), rs.getString("statut"), rs.getTimestamp("created_at").toInstant()), scopeArgs);
    }

    private List<ContactRow> recentContacts(String scope, Object[] scopeArgs) {
        return jdbc.query(
                "SELECT c.id, c.sale_listing_id, c.nom, c.email, c.message, c.statut, c.created_at FROM contact_requests c WHERE " + scope + " ORDER BY c.created_at DESC LIMIT 5",
                (rs, row) -> new ContactRow(rs.getLong("id"), rs.getLong("sale_listing_id"), rs.getString("nom"),
                        rs.getString("email"), rs.getString("message"), rs.getString("statut"), rs.getTimestamp("created_at").toInstant()), scopeArgs);
    }

    private long count(String sql, Object[] args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    private Object[] rangeArgs(LocalDate from, LocalDate to) {
        return new Object[]{Timestamp.from(from.atStartOfDay(BUSINESS_ZONE).toInstant()),
                Timestamp.from(to.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant())};
    }

    private Object[] prepend(Object[] first, Object[] second) {
        Object[] result = new Object[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
