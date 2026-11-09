package ma.locavite.reservation.service;

import ma.locavite.catalog.domain.Category;
import ma.locavite.reservation.config.PricingProperties;
import ma.locavite.reservation.domain.OptionPrice;
import ma.locavite.reservation.domain.ReservationOption;
import ma.locavite.reservation.domain.Season;
import ma.locavite.reservation.dto.PriceQuoteResponse;
import ma.locavite.reservation.repository.OptionPriceRepository;
import ma.locavite.vehicle.domain.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    private static final LocalDate START = LocalDate.of(2026, 6, 1);
    private static final BigDecimal DAILY_PRICE = new BigDecimal("400.00");

    @Mock
    private SeasonService seasonService;

    @Mock
    private OptionPriceRepository optionPriceRepository;

    private PricingProperties pricingProperties;
    private PricingService pricingService;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        pricingProperties = new PricingProperties();
        pricingProperties.setVatRate(new BigDecimal("20"));
        pricingService = new PricingService(seasonService, optionPriceRepository, pricingProperties);
        Category category = new Category();
        category.setCaution(new BigDecimal("3000.00"));
        vehicle = new Vehicle();
        vehicle.setPrixJour(DAILY_PRICE);
        vehicle.setCategory(category);

        lenient().when(seasonService.getAll()).thenReturn(List.of());
        lenient().when(optionPriceRepository.findAll()).thenReturn(List.of(
                optionPrice(ReservationOption.GPS, "50.00"),
                optionPrice(ReservationOption.BABY_SEAT, "30.00"),
                optionPrice(ReservationOption.EXTRA_DRIVER, "100.00")
        ));
    }

    @Test
    @DisplayName("Three days in low season use the base daily price")
    void calculate_lowSeason() {
        PriceQuoteResponse quote = calculate(3, Set.of());

        assertThat(quote.sousTotal()).isEqualByComparingTo("1200.00");
        assertThat(quote.options()).isEqualByComparingTo("0.00");
        assertThat(quote.tva()).isEqualByComparingTo("240.00");
        assertThat(quote.caution()).isEqualByComparingTo("3000.00");
        assertThat(quote.total()).isEqualByComparingTo("4440.00");
    }

    @Test
    @DisplayName("Three days in high season apply the configured 1.5 coefficient")
    void calculate_highSeason() {
        Season highSeason = season("Haute saison", START, START.plusDays(3), "1.5");
        when(seasonService.getAll()).thenReturn(List.of(highSeason));

        PriceQuoteResponse quote = calculate(3, Set.of());

        assertThat(quote.sousTotal()).isEqualByComparingTo("1800.00");
        assertThat(quote.total()).isEqualByComparingTo("5160.00");
    }

    @Test
    @DisplayName("Ten days receive a ten percent rental discount")
    void calculate_longRentalDiscount() {
        PriceQuoteResponse quote = calculate(10, Set.of());

        assertThat(quote.remise()).isEqualByComparingTo("400.00");
        assertThat(quote.tva()).isEqualByComparingTo("720.00");
        assertThat(quote.total()).isEqualByComparingTo("7320.00");
    }

    @Test
    @DisplayName("Thirty-one days receive a twenty percent rental discount")
    void calculate_veryLongRentalDiscount() {
        PriceQuoteResponse quote = calculate(31, Set.of());

        assertThat(quote.remise()).isEqualByComparingTo("2480.00");
        assertThat(quote.tva()).isEqualByComparingTo("1984.00");
        assertThat(quote.total()).isEqualByComparingTo("14904.00");
    }

    @Test
    @DisplayName("A reservation crossing seasons prices each day at its season rate")
    void calculate_reservationCrossingSeasons() {
        Season highSeason = season("Haute saison", START.plusDays(2), START.plusDays(4), "1.5");
        when(seasonService.getAll()).thenReturn(List.of(highSeason));

        PriceQuoteResponse quote = calculate(3, Set.of());

        assertThat(quote.sousTotal()).isEqualByComparingTo("1400.00");
        assertThat(quote.total()).isEqualByComparingTo("4680.00");
    }

    @Test
    @DisplayName("Options are charged per day at database prices")
    void calculate_options() {
        PriceQuoteResponse quote = calculate(3, Set.of(
                ReservationOption.GPS,
                ReservationOption.BABY_SEAT,
                ReservationOption.EXTRA_DRIVER
        ));

        assertThat(quote.options()).isEqualByComparingTo("540.00");
        assertThat(quote.tva()).isEqualByComparingTo("348.00");
        assertThat(quote.total()).isEqualByComparingTo("5088.00");
    }

    @Test
    @DisplayName("Quote calculation enforces required VAT configuration")
    void calculate_withoutConfiguredVat_throws() {
        pricingProperties.setVatRate(null);

        assertThatThrownBy(() -> calculate(3, Set.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("taux de TVA");
    }

    private PriceQuoteResponse calculate(int days, Set<ReservationOption> options) {
        return pricingService.calculate(vehicle, START, START.plusDays(days), options);
    }

    private OptionPrice optionPrice(ReservationOption option, String dailyPrice) {
        OptionPrice optionPrice = new OptionPrice();
        optionPrice.setType(option);
        optionPrice.setPrixJour(new BigDecimal(dailyPrice));
        return optionPrice;
    }

    private Season season(String name, LocalDate start, LocalDate end, String coefficient) {
        Season season = new Season();
        season.setName(name);
        season.setDateDebut(start);
        season.setDateFin(end);
        season.setCoefficient(new BigDecimal(coefficient));
        return season;
    }
}
