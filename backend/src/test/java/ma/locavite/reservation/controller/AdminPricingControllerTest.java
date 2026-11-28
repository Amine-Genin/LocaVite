package ma.locavite.reservation.controller;

import ma.locavite.reservation.domain.DiscountType;
import ma.locavite.reservation.domain.ReservationOption;
import ma.locavite.reservation.dto.*;
import ma.locavite.reservation.service.PricingAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminPricingControllerTest {

    @Mock
    private PricingAdminService pricingAdminService;

    private AdminPricingController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminPricingController(pricingAdminService);
    }

    @Test
    @DisplayName("CRUD endpoints /api/admin/options retournent les statuts HTTP attendus")
    void optionsCrudEndpoints() {
        OptionPriceResponse optionResp = new OptionPriceResponse(1L, ReservationOption.GPS, "GPS", new BigDecimal("50.00"), true);
        OptionPriceRequest req = new OptionPriceRequest(ReservationOption.GPS, "GPS", new BigDecimal("50.00"), true);

        when(pricingAdminService.getAllOptions()).thenReturn(List.of(optionResp));
        when(pricingAdminService.createOption(req)).thenReturn(optionResp);
        when(pricingAdminService.updateOption(1L, req)).thenReturn(optionResp);

        assertThat(controller.getAllOptions().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.createOption(req).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.updateOption(1L, req).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.deleteOption(1L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(pricingAdminService).deleteOption(1L);
    }

    @Test
    @DisplayName("CRUD endpoints /api/admin/discounts retournent les statuts HTTP attendus")
    void discountsCrudEndpoints() {
        DiscountResponse resp = new DiscountResponse(1L, DiscountType.LONG_DURATION, null, "Longue durée", 8, new BigDecimal("10.00"), null, null, true, null, null);
        DiscountRequest req = new DiscountRequest(DiscountType.LONG_DURATION, null, "Longue durée", 8, new BigDecimal("10.00"), null, null, true);

        when(pricingAdminService.getAllDiscounts()).thenReturn(List.of(resp));
        when(pricingAdminService.createDiscount(req)).thenReturn(resp);
        when(pricingAdminService.updateDiscount(1L, req)).thenReturn(resp);

        assertThat(controller.getAllDiscounts().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.createDiscount(req).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.updateDiscount(1L, req).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.deleteDiscount(1L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(pricingAdminService).deleteDiscount(1L);
    }

    @Test
    @DisplayName("POST /api/admin/pricing/simulate retourne la simulation (200 OK)")
    void simulateEndpoint_returnsSimulation() {
        PricingSimulateRequest req = new PricingSimulateRequest(
                1L, null, null, null,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5),
                Set.of(ReservationOption.GPS), null);
        PricingSimulateResponse resp = new PricingSimulateResponse(
                new BigDecimal("1600.00"), new BigDecimal("200.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("3000.00"), new BigDecimal("360.00"),
                new BigDecimal("5160.00"), 4, null);

        when(pricingAdminService.simulatePrice(req)).thenReturn(resp);

        ResponseEntity<PricingSimulateResponse> result = controller.simulatePrice(req);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().total()).isEqualByComparingTo("5160.00");
    }
}
