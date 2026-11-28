package ma.locavite.vehicle.service;

import ma.locavite.auth.repository.UserRepository;
import ma.locavite.payment.domain.Payment;
import ma.locavite.payment.domain.PaymentStatus;
import ma.locavite.payment.repository.PaymentRepository;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationStatus;
import ma.locavite.reservation.repository.ReservationRepository;
import ma.locavite.vehicle.domain.*;
import ma.locavite.vehicle.dto.*;
import ma.locavite.vehicle.exception.VehicleNotFoundException;
import ma.locavite.vehicle.repository.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class FleetService {
    private final VehicleRepository vehicles;
    private final VehicleStatusHistoryRepository statusHistory;
    private final VehicleMaintenanceRepository maintenance;
    private final VehicleIncidentRepository incidents;
    private final ReservationRepository reservations;
    private final PaymentRepository payments;
    private final UserRepository users;
    private final CurrentUserService currentUser;

    public FleetService(VehicleRepository vehicles, VehicleStatusHistoryRepository statusHistory,
            VehicleMaintenanceRepository maintenance, VehicleIncidentRepository incidents, ReservationRepository reservations,
            PaymentRepository payments, UserRepository users, CurrentUserService currentUser) {
        this.vehicles = vehicles; this.statusHistory = statusHistory; this.maintenance = maintenance; this.incidents = incidents;
        this.reservations = reservations; this.payments = payments; this.users = users; this.currentUser = currentUser;
    }

    public List<FleetVehicleResponse> getFleet(VehicleStatus status, String categorie, String ville) {
        Long agencyId = currentUser.isAdmin() ? null : currentUser.getCurrentUserId()
                .orElseThrow(() -> new AccessDeniedException("Utilisateur non authentifié"));
        List<Vehicle> fleet = vehicles.findAll().stream()
                .filter(v -> agencyId == null || agencyId.equals(v.getAgenceId()))
                .filter(v -> status == null || status == v.getStatut())
                .filter(v -> categorie == null || categorie.isBlank() || v.getCategory().getName().equalsIgnoreCase(categorie))
                .filter(v -> ville == null || ville.isBlank() || v.getVille().equalsIgnoreCase(ville))
                .toList();
        List<Reservation> allReservations = reservations.findAll();
        List<Payment> allPayments = payments.findAll();
        LocalDate today = LocalDate.now(); YearMonth month = YearMonth.now();
        return fleet.stream().map(vehicle -> toFleetRow(vehicle, allReservations, allPayments, today, month)).toList();
    }

    private FleetVehicleResponse toFleetRow(Vehicle v, List<Reservation> allReservations, List<Payment> allPayments,
                                             LocalDate today, YearMonth month) {
        List<Reservation> own = allReservations.stream().filter(r -> r.getVehicleId().equals(v.getId())).toList();
        Reservation next = own.stream().filter(r -> !r.getDateDebut().isBefore(today))
                .filter(r -> r.getStatut() == ReservationStatus.PENDING || r.getStatut() == ReservationStatus.CONFIRMED)
                .min(Comparator.comparing(Reservation::getDateDebut)).orElse(null);
        Map<Long, Reservation> reservationById = new HashMap<>(); own.forEach(r -> reservationById.put(r.getId(), r));
        BigDecimal monthRevenue = BigDecimal.ZERO, yearRevenue = BigDecimal.ZERO;
        for (Payment payment : allPayments) {
            Reservation r = reservationById.get(payment.getReservationId());
            if (r == null || payment.getStatut() != PaymentStatus.SUCCESS) continue;
            LocalDate paid = payment.getPaidAt() != null ? payment.getPaidAt().atZone(ZoneId.systemDefault()).toLocalDate()
                    : payment.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
            if (YearMonth.from(paid).equals(month)) monthRevenue = monthRevenue.add(payment.getMontant());
            if (paid.getYear() == today.getYear()) yearRevenue = yearRevenue.add(payment.getMontant());
        }
        LocalDate yearStart = LocalDate.of(today.getYear(), 1, 1);
        LocalDate yearEnd = yearStart.plusYears(1);
        long rentedDays = own.stream()
                .filter(r -> r.getStatut() == ReservationStatus.CONFIRMED
                        || r.getStatut() == ReservationStatus.ACTIVE
                        || r.getStatut() == ReservationStatus.COMPLETED)
                .mapToLong(r -> {
                    LocalDate start = r.getDateDebut().isAfter(yearStart) ? r.getDateDebut() : yearStart;
                    LocalDate end = r.getDateFin().isBefore(yearEnd) ? r.getDateFin() : yearEnd;
                    return Math.max(0, Duration.between(start.atStartOfDay(), end.atStartOfDay()).toDays());
                }).sum();
        double occupancy = Math.min(100d, Math.round(rentedDays * 10000d / today.lengthOfYear()) / 100d);
        String location = own.stream().filter(r -> r.getStatut() == ReservationStatus.ACTIVE).findFirst()
                .map(Reservation::getLieuPriseEnCharge).orElse(v.getVille());
        return new FleetVehicleResponse(v.getId(), v.getMatricule(), v.getBrand().getName(), v.getModele(), v.getAnnee(),
                v.getCategory().getName(), v.getStatut(), v.getVille(), location, next == null ? null : next.getId(),
                next == null ? null : next.getDateDebut(), v.getKilometrage(), v.getDerniereMaintenance(),
                v.getProchaineMaintenance(), monthRevenue, yearRevenue, occupancy, v.getAgenceId(), v.getCreatedAt());
    }

    @Transactional
    @Caching(evict = { @CacheEvict(value = "vehicleSearch", allEntries = true), @CacheEvict(value = "vehicles", allEntries = true) })
    public FleetVehicleResponse changeStatus(Long id, VehicleStatusRequest request) {
        if (!EnumSet.of(VehicleStatus.PUBLISHED, VehicleStatus.RENTED, VehicleStatus.MAINTENANCE, VehicleStatus.ARCHIVED).contains(request.statut()))
            throw new IllegalArgumentException("Statut de flotte non autorisé");
        Vehicle v = manageable(id); VehicleStatus old = v.getStatut();
        if (old != request.statut()) {
            VehicleStatusHistory event = new VehicleStatusHistory(); event.setVehicleId(id); event.setAncienStatut(old);
            event.setNouveauStatut(request.statut()); event.setRaison(request.raison());
            event.setDate(Instant.now()); event.setUserId(currentUser.getCurrentUserId().orElseThrow()); statusHistory.save(event);
            v.setStatut(request.statut()); vehicles.save(v);
        }
        return getFleet(null, null, null).stream().filter(row -> row.id().equals(id)).findFirst().orElseThrow();
    }

    @Transactional
    @Caching(evict = { @CacheEvict(value = "vehicleSearch", allEntries = true), @CacheEvict(value = "vehicles", allEntries = true) })
    public FleetVehicleResponse transfer(Long id, VehicleTransferRequest request) {
        Vehicle v = vehicles.findById(id).orElseThrow(() -> new VehicleNotFoundException(id));
        if (!currentUser.isAdmin()) throw new AccessDeniedException("Transfert réservé aux administrateurs");
        if (!users.existsById(request.agenceId())) throw new IllegalArgumentException("Agence introuvable");
        Long previousAgency = v.getAgenceId();
        VehicleStatusHistory event = new VehicleStatusHistory(); event.setVehicleId(id); event.setAncienStatut(v.getStatut());
        event.setNouveauStatut(v.getStatut()); event.setRaison("Transfert d'agence " + previousAgency + " → " + request.agenceId());
        event.setDate(Instant.now()); event.setUserId(currentUser.getCurrentUserId().orElseThrow()); statusHistory.save(event);
        v.setAgenceId(request.agenceId()); vehicles.save(v);
        return getFleet(null, null, null).stream().filter(row -> row.id().equals(id)).findFirst()
                .orElseGet(() -> toFleetRow(v, reservations.findAll(), payments.findAll(), LocalDate.now(), YearMonth.now()));
    }

    @Transactional
    public FleetHistoryResponse addMaintenance(Long id, VehicleMaintenanceRequest request) {
        Vehicle v = manageable(id);
        Long userId = currentUser.getCurrentUserId().orElseThrow();
        VehicleMaintenance item = new VehicleMaintenance(); item.setVehicleId(id); item.setDate(request.date());
        item.setDescription(request.description()); item.setKilometrage(request.kilometrage());
        item.setProchaineMaintenance(request.prochaineMaintenance()); item.setCreatedBy(userId); item.setCreatedAt(Instant.now());
        maintenance.save(item); v.setKilometrage(request.kilometrage()); v.setDerniereMaintenance(request.date());
        v.setProchaineMaintenance(request.prochaineMaintenance()); vehicles.save(v);
        return new FleetHistoryResponse("MAINTENANCE", item.getDescription(), item.getCreatedAt(), item.getDate(), null, null, item.getKilometrage(), item.getId());
    }

    @Transactional
    public FleetHistoryResponse addIncident(Long id, VehicleIncidentRequest request) {
        manageable(id);
        VehicleIncident item = new VehicleIncident(); item.setVehicleId(id); item.setDate(request.date());
        item.setDescription(request.description()); item.setGravite(request.gravite().toUpperCase(Locale.ROOT));
        item.setCreatedBy(currentUser.getCurrentUserId().orElseThrow()); item.setCreatedAt(Instant.now()); incidents.save(item);
        return new FleetHistoryResponse("INCIDENT", item.getGravite() + " · " + item.getDescription(), item.getCreatedAt(), item.getDate(), null, null, null, item.getId());
    }

    public List<FleetHistoryResponse> getHistory(Long id) {
        Vehicle vehicle = manageable(id);
        List<FleetHistoryResponse> history = new ArrayList<>();
        statusHistory.findByVehicleIdOrderByDateDesc(id).forEach(e -> history.add(new FleetHistoryResponse(e.getRaison() != null && e.getRaison().startsWith("Transfert d'agence") ? "TRANSFERT" : "STATUT",
                e.getRaison(), e.getDate(), e.getDate().atZone(ZoneId.systemDefault()).toLocalDate(),
                e.getAncienStatut() == null ? null : e.getAncienStatut().name(), e.getNouveauStatut().name(), null, e.getId())));
        reservations.findByVehicleIdOrderByDateDebutAsc(id).forEach(r -> history.add(new FleetHistoryResponse("RESERVATION",
                "Réservation " + r.getCode() + " · " + r.getStatut(), r.getCreatedAt(), r.getDateDebut(), null, r.getStatut().name(), null, r.getId())));
        maintenance.findByVehicleIdOrderByDateDesc(id).forEach(m -> history.add(new FleetHistoryResponse("MAINTENANCE",
                m.getDescription(), m.getCreatedAt(), m.getDate(), null, null, m.getKilometrage(), m.getId())));
        incidents.findByVehicleIdOrderByDateDesc(id).forEach(i -> history.add(new FleetHistoryResponse("INCIDENT",
                i.getGravite() + " · " + i.getDescription(), i.getCreatedAt(), i.getDate(), null, null, null, i.getId())));
        return history.stream().sorted(Comparator.comparing(FleetHistoryResponse::dateHeure,
                Comparator.nullsLast(Comparator.reverseOrder()))).toList();
    }

    private Vehicle manageable(Long id) {
        Vehicle v = vehicles.findById(id).orElseThrow(() -> new VehicleNotFoundException(id));
        if (!currentUser.isAdmin() && !currentUser.getCurrentUserId().filter(v.getAgenceId()::equals).isPresent())
            throw new AccessDeniedException("Vous n'êtes pas propriétaire de ce véhicule");
        return v;
    }
}
