package ma.locavite.reservation.service;

import ma.locavite.catalog.domain.Category;
import ma.locavite.reservation.config.PricingProperties;
import ma.locavite.reservation.domain.*;
import ma.locavite.reservation.dto.*;
import ma.locavite.reservation.exception.SeasonOverlapException;
import ma.locavite.reservation.repository.DiscountRepository;
import ma.locavite.reservation.repository.OptionPriceRepository;
import ma.locavite.reservation.repository.SeasonRepository;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PricingAdminServiceTest {

    @Mock
    private SeasonRepository seasonRepository;
    @Mock
    private OptionPriceRepository optionPriceRepository;
    @Mock
    private DiscountRepository discountRepository;
    @Mock
    private VehicleRepository vehicleRepository;

    private SeasonService seasonService;
    private PricingService pricingService;
    private PricingAdminService pricingAdminService;

    @BeforeEach
    void setUp() {
        PricingProperties pricingProperties = new PricingProperties();
        pricingProperties.setVatRate(new BigDecimal("20"));

        seasonService = new SeasonService(seasonRepository, null);
        pricingService = new PricingService(seasonService, optionPriceRepository, pricingProperties, discountRepository);
        pricingAdminService = new PricingAdminService(
                seasonService,
                optionPriceRepository,
                discountRepository,
                vehicleRepository,
                pricingService,
                pricingProperties
        );
    }

    @Test
    @DisplayName("Création saison valide via PricingAdminService")
    void createSeason_valid_returnsCreatedSeason() {
        when(seasonRepository.findAll()).thenReturn(List.of());
        when(seasonRepository.save(any(Season.class))).thenAnswer(inv -> {
            Season s = inv.getArgument(0);
            s.setId(1L);
            return s;
        });

        SeasonRequest request = new SeasonRequest(
                "Haute saison été",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 31),
                new BigDecimal("1.50"),
                3,
                10L,
                null,
                true
        );

        SeasonResponse response = pricingAdminService.createSeason(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNom()).isEqualTo("Haute saison été");
        assertThat(response.getCoefficient()).isEqualByComparingTo("1.50");
        assertThat(response.getPriorite()).isEqualTo(3);
        assertThat(response.getAgenceId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Chevauchement de saison détecté sans priorité distincte")
    void createSeason_overlapWithoutDistinctPriority_throwsException() {
        Season existing = new Season("Été", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31), new BigDecimal("1.50"), 2);
        existing.setId(1L);
        when(seasonRepository.findAll()).thenReturn(List.of(existing));

        SeasonRequest request = new SeasonRequest(
                "Été bis",
                LocalDate.of(2026, 7, 15),
                LocalDate.of(2026, 9, 10),
                new BigDecimal("1.70"),
                2,
                null,
                null,
                false
        );

        assertThatThrownBy(() -> pricingAdminService.createSeason(request))
                .isInstanceOf(SeasonOverlapException.class)
                .hasMessageContaining("Chevauchement détecté");
    }

    @Test
    @DisplayName("CRUD complet sur les options tarifaires")
    void optionCrud_createReadUpdateDelete() {
        OptionPrice gps = new OptionPrice();
        gps.setId(1L);
        gps.setType(ReservationOption.GPS);
        gps.setLibelle("GPS Navigation");
        gps.setPrixJour(new BigDecimal("50.00"));
        gps.setActif(true);

        // Create
        when(optionPriceRepository.findByType(ReservationOption.GPS)).thenReturn(Optional.empty());
        when(optionPriceRepository.save(any(OptionPrice.class))).thenAnswer(inv -> {
            OptionPrice saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        OptionPriceResponse created = pricingAdminService.createOption(
                new OptionPriceRequest(ReservationOption.GPS, "GPS Navigation", new BigDecimal("50.00"), true));
        assertThat(created.id()).isEqualTo(1L);
        assertThat(created.libelle()).isEqualTo("GPS Navigation");
        assertThat(created.prixJour()).isEqualByComparingTo("50.00");
        assertThat(created.actif()).isTrue();

        // Read
        when(optionPriceRepository.findAll()).thenReturn(List.of(gps));
        assertThat(pricingAdminService.getAllOptions()).hasSize(1);

        // Update
        when(optionPriceRepository.findById(1L)).thenReturn(Optional.of(gps));
        when(optionPriceRepository.findByType(ReservationOption.GPS)).thenReturn(Optional.of(gps));
        OptionPriceResponse updated = pricingAdminService.updateOption(
                1L,
                new OptionPriceRequest(ReservationOption.GPS, "GPS Pro HD", new BigDecimal("65.00"), false));
        assertThat(updated.libelle()).isEqualTo("GPS Pro HD");
        assertThat(updated.prixJour()).isEqualByComparingTo("65.00");
        assertThat(updated.actif()).isFalse();

        // Delete
        pricingAdminService.deleteOption(1L);
        verify(optionPriceRepository).delete(gps);
    }

    @Test
    @DisplayName("CRUD remise code promo : création et détection de doublon")
    void discountCrud_promoCode() {
        when(discountRepository.existsByCodeIgnoreCase("SUMMER15")).thenReturn(false);
        when(discountRepository.save(any(Discount.class))).thenAnswer(inv -> {
            Discount d = inv.getArgument(0);
            d.setId(20L);
            return d;
        });

        DiscountResponse created = pricingAdminService.createDiscount(new DiscountRequest(
                DiscountType.PROMO_CODE,
                "summer15",
                "Promo Été 15%",
                null,
                new BigDecimal("15.00"),
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 31),
                true
        ));

        assertThat(created.id()).isEqualTo(20L);
        assertThat(created.code()).isEqualTo("SUMMER15");
        assertThat(created.pourcentage()).isEqualByComparingTo("15.00");

        when(discountRepository.existsByCodeIgnoreCase("SUMMER15")).thenReturn(true);
        assertThatThrownBy(() -> pricingAdminService.createDiscount(new DiscountRequest(
                DiscountType.PROMO_CODE,
                "SUMMER15",
                "Doublon",
                null,
                new BigDecimal("10.00"),
                null,
                null,
                true
        ))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Simulation de prix correcte avec saison, options, remise longue durée et code promo")
    void simulatePrice_calculatesSeasonOptionsDurationAndPromo() {
        Category category = new Category();
        category.setCaution(new BigDecimal("3000.00"));

        Vehicle vehicle = new Vehicle();
        vehicle.setId(42L);
        vehicle.setPrixJour(new BigDecimal("400.00"));
        vehicle.setCategory(category);
        when(vehicleRepository.findById(42L)).thenReturn(Optional.of(vehicle));

        Season highSeason = new Season("Été", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31), new BigDecimal("1.50"), 3);
        when(seasonRepository.findAllOrderedByPriority()).thenReturn(List.of(highSeason));

        OptionPrice gps = new OptionPrice();
        gps.setType(ReservationOption.GPS);
        gps.setLibelle("GPS");
        gps.setPrixJour(new BigDecimal("50.00"));
        gps.setActif(true);
        when(optionPriceRepository.findAll()).thenReturn(List.of(gps));

        Discount longDuration = new Discount();
        longDuration.setType(DiscountType.LONG_DURATION);
        longDuration.setMinDays(8);
        longDuration.setPourcentage(new BigDecimal("10.00"));
        longDuration.setActif(true);
        when(discountRepository.findByTypeAndActifTrueOrderByMinDaysDesc(DiscountType.LONG_DURATION))
                .thenReturn(List.of(longDuration));

        Discount promo = new Discount();
        promo.setType(DiscountType.PROMO_CODE);
        promo.setCode("WELCOME5");
        promo.setPourcentage(new BigDecimal("5.00"));
        promo.setActif(true);
        when(discountRepository.findByCodeIgnoreCase("WELCOME5")).thenReturn(Optional.of(promo));

        // 10 jours * 400 * 1.50 = 6000.00 sous-total
        // Options GPS: 10 * 50 = 500.00
        // Remise durée (10%): 600.00
        // Remise promo (5%): 300.00 -> remiseTotale = 900.00
        // Taxable: 6000 - 900 + 500 = 5600.00
        // TVA (20%): 1120.00
        // Caution: 3000.00
        // Total: 5600 + 1120 + 3000 = 9720.00
        PricingSimulateResponse result = pricingAdminService.simulatePrice(new PricingSimulateRequest(
                42L,
                null,
                null,
                null,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 11),
                Set.of(ReservationOption.GPS),
                "welcome5"
        ));

        assertThat(result.jours()).isEqualTo(10);
        assertThat(result.sousTotal()).isEqualByComparingTo("6000.00");
        assertThat(result.options()).isEqualByComparingTo("500.00");
        assertThat(result.remiseDuree()).isEqualByComparingTo("600.00");
        assertThat(result.remisePromo()).isEqualByComparingTo("300.00");
        assertThat(result.remiseTotale()).isEqualByComparingTo("900.00");
        assertThat(result.tva()).isEqualByComparingTo("1120.00");
        assertThat(result.caution()).isEqualByComparingTo("3000.00");
        assertThat(result.total()).isEqualByComparingTo("9720.00");
        assertThat(result.promoCodeApplique()).isEqualTo("WELCOME5");
    }
}
