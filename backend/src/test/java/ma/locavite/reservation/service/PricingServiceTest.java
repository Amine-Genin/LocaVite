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

    @Test
    @DisplayName("Test : 15 juillet → haute saison (×1.5)")
    void test15JuilletHauteSaison() {
        Season basseSaison = season("Basse saison", LocalDate.of(2026, 10, 1), LocalDate.of(2027, 3, 31), "1.0");
        basseSaison.setPriorite(1);
        Season hauteSaison = season("Haute saison été", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31), "1.5");
        hauteSaison.setPriorite(3);

        when(seasonService.getAll()).thenReturn(List.of(basseSaison, hauteSaison));

        LocalDate date15Juillet = LocalDate.of(2026, 7, 15);
        BigDecimal coeff = pricingService.resolveCoefficient(vehicle, date15Juillet);
        assertThat(coeff).isEqualByComparingTo("1.50");

        PriceQuoteResponse quote = pricingService.calculate(vehicle, date15Juillet, date15Juillet.plusDays(1), Set.of());
        assertThat(quote.sousTotal()).isEqualByComparingTo("600.00"); // 400 * 1.5
    }

    @Test
    @DisplayName("Test : 15 novembre → basse saison (×1.0)")
    void test15NovembreBasseSaison() {
        Season basseSaison = season("Basse saison", LocalDate.of(2026, 10, 1), LocalDate.of(2027, 3, 31), "1.0");
        basseSaison.setPriorite(1);
        Season hauteSaison = season("Haute saison été", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31), "1.5");
        hauteSaison.setPriorite(3);

        when(seasonService.getAll()).thenReturn(List.of(basseSaison, hauteSaison));

        LocalDate date15Novembre = LocalDate.of(2026, 11, 15);
        BigDecimal coeff = pricingService.resolveCoefficient(vehicle, date15Novembre);
        assertThat(coeff).isEqualByComparingTo("1.00");

        PriceQuoteResponse quote = pricingService.calculate(vehicle, date15Novembre, date15Novembre.plusDays(1), Set.of());
        assertThat(quote.sousTotal()).isEqualByComparingTo("400.00"); // 400 * 1.0
    }

    @Test
    @DisplayName("Test : Aïd → très haute (×2.0)")
    void testAidTresHauteSaison() {
        Season aid = season("Aïd el-Kébir", LocalDate.of(2026, 5, 25), LocalDate.of(2026, 6, 5), "2.0");
        aid.setPriorite(4);
        Season moyenne = season("Moyenne saison", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30), "1.2");
        moyenne.setPriorite(2);

        when(seasonService.getAll()).thenReturn(List.of(moyenne, aid));

        LocalDate dateAid = LocalDate.of(2026, 5, 28);
        BigDecimal coeff = pricingService.resolveCoefficient(vehicle, dateAid);
        assertThat(coeff).isEqualByComparingTo("2.00");

        PriceQuoteResponse quote = pricingService.calculate(vehicle, dateAid, dateAid.plusDays(1), Set.of());
        assertThat(quote.sousTotal()).isEqualByComparingTo("800.00"); // 400 * 2.0
    }

    @Test
    @DisplayName("Test : override agence")
    void testOverrideAgence() {
        LocalDate date15Juillet = LocalDate.of(2026, 7, 15);

        // Saison globale : 1.5
        Season globalHigh = season("Haute saison globale", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31), "1.5");
        globalHigh.setPriorite(3);

        // Override Agence 99 : 1.8
        Season agenceOverride = season("Override Agence 99", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31), "1.8");
        agenceOverride.setAgenceId(99L);
        agenceOverride.setPriorite(3);

        when(seasonService.getAll()).thenReturn(List.of(globalHigh, agenceOverride));

        // Véhicule de l'Agence 99
        Vehicle vehicleAgence99 = new Vehicle();
        vehicleAgence99.setId(10L);
        vehicleAgence99.setAgenceId(99L);
        vehicleAgence99.setPrixJour(DAILY_PRICE);
        vehicleAgence99.setCategory(vehicle.getCategory());

        // Véhicule d'une autre agence (Agence 88)
        Vehicle vehicleOtherAgence = new Vehicle();
        vehicleOtherAgence.setId(20L);
        vehicleOtherAgence.setAgenceId(88L);
        vehicleOtherAgence.setPrixJour(DAILY_PRICE);
        vehicleOtherAgence.setCategory(vehicle.getCategory());

        // L'agence 99 bénéficie de son override 1.80
        BigDecimal coeffAgence99 = pricingService.resolveCoefficient(vehicleAgence99, date15Juillet);
        assertThat(coeffAgence99).isEqualByComparingTo("1.80");

        PriceQuoteResponse quoteAgence99 = pricingService.calculate(vehicleAgence99, date15Juillet, date15Juillet.plusDays(1), Set.of());
        assertThat(quoteAgence99.sousTotal()).isEqualByComparingTo("720.00"); // 400 * 1.8

        // L'autre agence applique la saison globale 1.50
        BigDecimal coeffOther = pricingService.resolveCoefficient(vehicleOtherAgence, date15Juillet);
        assertThat(coeffOther).isEqualByComparingTo("1.50");

        PriceQuoteResponse quoteOther = pricingService.calculate(vehicleOtherAgence, date15Juillet, date15Juillet.plusDays(1), Set.of());
        assertThat(quoteOther.sousTotal()).isEqualByComparingTo("600.00"); // 400 * 1.5
    }

    @Test
    @DisplayName("Test : override spécifique au véhicule l'emporte sur l'override agence et global")
    void testOverrideVehicule() {
        LocalDate date15Juillet = LocalDate.of(2026, 7, 15);

        Season globalHigh = season("Haute saison globale", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31), "1.5");
        Season agenceOverride = season("Override Agence 99", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31), "1.8");
        agenceOverride.setAgenceId(99L);
        Season vehicleOverride = season("Override Véhicule VIP", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31), "2.2");
        vehicleOverride.setVehicleId(10L);

        when(seasonService.getAll()).thenReturn(List.of(globalHigh, agenceOverride, vehicleOverride));

        Vehicle vipVehicle = new Vehicle();
        vipVehicle.setId(10L);
        vipVehicle.setAgenceId(99L);
        vipVehicle.setPrixJour(DAILY_PRICE);
        vipVehicle.setCategory(vehicle.getCategory());

        BigDecimal coeff = pricingService.resolveCoefficient(vipVehicle, date15Juillet);
        assertThat(coeff).isEqualByComparingTo("2.20");
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
