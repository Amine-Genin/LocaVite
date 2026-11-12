package ma.locavite.reservation.service;

import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.notification.service.NotificationService;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationStatus;
import ma.locavite.reservation.domain.Season;
import ma.locavite.reservation.dto.PriceQuoteResponse;
import ma.locavite.reservation.event.SeasonChangedEvent;
import ma.locavite.reservation.repository.ReservationRepository;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class SeasonChangedListener {

    private static final Logger log = LoggerFactory.getLogger(SeasonChangedListener.class);

    private final ReservationRepository reservationRepository;
    private final VehicleRepository vehicleRepository;
    private final PricingService pricingService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public SeasonChangedListener(
            ReservationRepository reservationRepository,
            VehicleRepository vehicleRepository,
            PricingService pricingService,
            NotificationService notificationService,
            UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.vehicleRepository = vehicleRepository;
        this.pricingService = pricingService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @Async
    @EventListener
    @Transactional
    public void onSeasonChanged(SeasonChangedEvent event) {
        if (event == null || event.season() == null) {
            return;
        }

        Season season = event.season();
        log.info("Traitement de la mise à jour saisonnière [seasonId={}, nom={}, action={}] pour le recalcul des prix futurs",
                season.getId(), season.getNom(), event.action());

        try {
            recalculateFutureReservations(season);
        } catch (Exception e) {
            log.error("Erreur lors du recalcul des prix des réservations futures suite au changement de saison : {}",
                    e.getMessage(), e);
        }
    }

    public int recalculateFutureReservations(Season season) {
        LocalDate today = LocalDate.now();
        List<Reservation> futureReservations = reservationRepository.findAll().stream()
                .filter(r -> (r.getStatut() == ReservationStatus.CONFIRMED || r.getStatut() == ReservationStatus.PENDING)
                        && r.getDateDebut() != null
                        && !r.getDateDebut().isBefore(today))
                .toList();

        int updatedCount = 0;
        for (Reservation reservation : futureReservations) {
            if (!isReservationAffected(reservation, season)) {
                continue;
            }

            Vehicle vehicle = vehicleRepository.findById(reservation.getVehicleId()).orElse(null);
            if (vehicle == null) {
                continue;
            }

            try {
                PriceQuoteResponse newQuote = pricingService.calculate(
                        vehicle,
                        reservation.getDateDebut(),
                        reservation.getDateFin(),
                        reservation.getOptions()
                );

                BigDecimal oldTotal = reservation.getPrixTotal();
                if (oldTotal == null || newQuote.total().compareTo(oldTotal) != 0) {
                    log.info("Recalcul du prix de la réservation {} : ancien={}, nouveau={}",
                            reservation.getCode(), oldTotal, newQuote.total());

                    reservation.setPrixTotal(newQuote.total());
                    reservationRepository.save(reservation);
                    updatedCount++;

                    notifyClientOfPriceChange(reservation, oldTotal, newQuote.total());
                }
            } catch (Exception e) {
                log.warn("Impossible de recalculer le prix pour la réservation {} : {}",
                        reservation.getId(), e.getMessage());
            }
        }

        log.info("Recalcul des prix terminé : {} réservations mises à jour", updatedCount);
        return updatedCount;
    }

    private boolean isReservationAffected(Reservation reservation, Season season) {
        if (season.isVehicleOverride()) {
            return season.getVehicleId().equals(reservation.getVehicleId());
        }
        if (season.isAgencyOverride()) {
            Vehicle vehicle = vehicleRepository.findById(reservation.getVehicleId()).orElse(null);
            return vehicle != null && season.getAgenceId().equals(vehicle.getAgenceId());
        }
        return true;
    }

    private void notifyClientOfPriceChange(Reservation reservation, BigDecimal oldPrice, BigDecimal newPrice) {
        if (notificationService == null || reservation.getUserId() == null) {
            return;
        }

        try {
            User client = userRepository.findById(reservation.getUserId()).orElse(null);
            if (client == null || !client.isNotificationOptedIn()) {
                return;
            }

            String message = String.format(
                    "LocaVite : Le tarif de votre réservation %s a été mis à jour suite au changement de saison. Nouveau total : %s MAD (ancien : %s MAD).",
                    reservation.getCode(),
                    newPrice != null ? newPrice.toPlainString() : "0.00",
                    oldPrice != null ? oldPrice.toPlainString() : "0.00"
            );

            log.info("Notification client envoyée suite au réajustement tarifaire pour la réservation {}", reservation.getCode());
            notificationService.sendNotification(reservation, ma.locavite.notification.domain.NotificationType.CONFIRMATION);
        } catch (Exception e) {
            log.error("Erreur lors de la notification du client pour réajustement tarifaire : {}", e.getMessage());
        }
    }
}
