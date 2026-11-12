package ma.locavite.reservation.service;

import ma.locavite.reservation.config.PricingProperties;
import ma.locavite.reservation.domain.OptionPrice;
import ma.locavite.reservation.domain.ReservationOption;
import ma.locavite.reservation.domain.Season;
import ma.locavite.reservation.dto.PriceQuoteResponse;
import ma.locavite.reservation.repository.OptionPriceRepository;
import ma.locavite.vehicle.domain.Vehicle;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PricingService {

    private static final BigDecimal LOW_SEASON_COEFFICIENT = BigDecimal.ONE;
    private static final BigDecimal LONG_RENTAL_DISCOUNT = new BigDecimal("0.10");
    private static final BigDecimal VERY_LONG_RENTAL_DISCOUNT = new BigDecimal("0.20");
    private static final BigDecimal VAT_DIVISOR = new BigDecimal("100");

    private final SeasonService seasonService;
    private final OptionPriceRepository optionPriceRepository;
    private final PricingProperties pricingProperties;

    public PricingService(
            SeasonService seasonService,
            OptionPriceRepository optionPriceRepository,
            PricingProperties pricingProperties) {
        this.seasonService = seasonService;
        this.optionPriceRepository = optionPriceRepository;
        this.pricingProperties = pricingProperties;
    }

    public PriceQuoteResponse calculate(
            Vehicle vehicle,
            LocalDate dateDebut,
            LocalDate dateFin,
            Set<ReservationOption> options) {
        if (dateDebut == null || dateFin == null || !dateDebut.isBefore(dateFin)) {
            throw new IllegalArgumentException("La date de début doit être antérieure à la date de fin");
        }
        if (vehicle.getCategory() == null || vehicle.getCategory().getCaution() == null) {
            throw new IllegalStateException("La caution doit être configurée pour la catégorie du véhicule");
        }

        long jours = ChronoUnit.DAYS.between(dateDebut, dateFin);
        List<Season> seasons = seasonService.getAll();
        BigDecimal sousTotal = calculateSeasonalSubtotal(vehicle, dateDebut, dateFin, seasons);
        BigDecimal optionsTotal = calculateOptions(options, jours);
        BigDecimal remiseRate = jours > 30
                ? VERY_LONG_RENTAL_DISCOUNT
                : jours > 7 ? LONG_RENTAL_DISCOUNT : BigDecimal.ZERO;
        BigDecimal remise = sousTotal.multiply(remiseRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxableAmount = sousTotal.subtract(remise).add(optionsTotal);
        BigDecimal tva = taxableAmount
                .multiply(pricingProperties.requireVatRate())
                .divide(VAT_DIVISOR, 2, RoundingMode.HALF_UP);
        BigDecimal caution = scale(vehicle.getCategory().getCaution());
        BigDecimal total = taxableAmount.add(tva).add(caution).setScale(2, RoundingMode.HALF_UP);

        return new PriceQuoteResponse(
                scale(sousTotal),
                scale(optionsTotal),
                remise,
                caution,
                tva,
                total,
                jours
        );
    }

    public BigDecimal resolveCoefficient(Vehicle vehicle, LocalDate date) {
        return resolveCoefficientForDay(vehicle, date, seasonService.getAll());
    }

    public BigDecimal resolveCoefficient(Long agenceId, Long vehicleId, LocalDate date) {
        Vehicle v = new Vehicle();
        v.setId(vehicleId);
        v.setAgenceId(agenceId);
        return resolveCoefficient(v, date);
    }

    public BigDecimal resolveCoefficientForDay(Vehicle vehicle, LocalDate day, java.util.List<Season> seasons) {
        if (seasons == null || seasons.isEmpty()) {
            return LOW_SEASON_COEFFICIENT;
        }

        Long vehicleId = (vehicle != null) ? vehicle.getId() : null;
        Long agenceId = (vehicle != null) ? vehicle.getAgenceId() : null;

        // 1. Override spécifique au véhicule
        if (vehicleId != null) {
            java.util.Optional<BigDecimal> vehicleCoeff = seasons.stream()
                    .filter(s -> vehicleId.equals(s.getVehicleId()) && s.matchesDate(day))
                    .max((s1, s2) -> {
                        int p = Integer.compare(s1.getPriorite(), s2.getPriorite());
                        return p != 0 ? p : s1.getCoefficient().compareTo(s2.getCoefficient());
                    })
                    .map(Season::getCoefficient);
            if (vehicleCoeff.isPresent()) {
                return vehicleCoeff.get();
            }
        }

        // 2. Override agence
        if (agenceId != null) {
            java.util.Optional<BigDecimal> agenceCoeff = seasons.stream()
                    .filter(s -> agenceId.equals(s.getAgenceId()) && s.getVehicleId() == null && s.matchesDate(day))
                    .max((s1, s2) -> {
                        int p = Integer.compare(s1.getPriorite(), s2.getPriorite());
                        return p != 0 ? p : s1.getCoefficient().compareTo(s2.getCoefficient());
                    })
                    .map(Season::getCoefficient);
            if (agenceCoeff.isPresent()) {
                return agenceCoeff.get();
            }
        }

        // 3. Saisons globales (agenceId IS NULL et vehicleId IS NULL)
        return seasons.stream()
                .filter(s -> s.isGlobal() && s.matchesDate(day))
                .max((s1, s2) -> {
                    int p = Integer.compare(s1.getPriorite(), s2.getPriorite());
                    return p != 0 ? p : s1.getCoefficient().compareTo(s2.getCoefficient());
                })
                .map(Season::getCoefficient)
                .orElse(LOW_SEASON_COEFFICIENT);
    }

    private BigDecimal calculateSeasonalSubtotal(
            Vehicle vehicle,
            LocalDate dateDebut,
            LocalDate dateFin,
            List<Season> seasons) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal prixJour = vehicle != null && vehicle.getPrixJour() != null ? vehicle.getPrixJour() : BigDecimal.ZERO;
        for (LocalDate day = dateDebut; day.isBefore(dateFin); day = day.plusDays(1)) {
            BigDecimal coefficient = resolveCoefficientForDay(vehicle, day, seasons);
            subtotal = subtotal.add(prixJour.multiply(coefficient));
        }
        return subtotal.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateOptions(Set<ReservationOption> options, long jours) {
        if (options == null || options.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        Map<ReservationOption, BigDecimal> prices = new EnumMap<>(ReservationOption.class);
        for (OptionPrice optionPrice : optionPriceRepository.findAll()) {
            prices.put(optionPrice.getType(), optionPrice.getPrixJour());
        }

        BigDecimal total = BigDecimal.ZERO;
        for (ReservationOption option : options) {
            BigDecimal prixJour = prices.get(option);
            if (prixJour == null) {
                throw new IllegalStateException("Aucun prix journalier configuré pour l'option " + option);
            }
            total = total.add(prixJour.multiply(BigDecimal.valueOf(jours)));
        }
        return scale(total);
    }

    private BigDecimal scale(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
