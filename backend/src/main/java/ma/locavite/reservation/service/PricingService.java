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
        BigDecimal sousTotal = calculateSeasonalSubtotal(vehicle.getPrixJour(), dateDebut, dateFin, seasons);
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

    private BigDecimal calculateSeasonalSubtotal(
            BigDecimal prixJour,
            LocalDate dateDebut,
            LocalDate dateFin,
            List<Season> seasons) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (LocalDate day = dateDebut; day.isBefore(dateFin); day = day.plusDays(1)) {
            LocalDate currentDay = day;
            BigDecimal coefficient = seasons.stream()
                    .filter(season -> !currentDay.isBefore(season.getDateDebut())
                            && currentDay.isBefore(season.getDateFin()))
                    .map(Season::getCoefficient)
                    .max(BigDecimal::compareTo)
                    .orElse(LOW_SEASON_COEFFICIENT);
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
