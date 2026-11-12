package ma.locavite.reservation.controller;

import ma.locavite.reservation.dto.SeasonRequest;
import ma.locavite.reservation.dto.SeasonResponse;
import ma.locavite.reservation.service.SeasonService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeasonControllerTest {

    @Mock
    private SeasonService seasonService;

    private SeasonController seasonController;
    private AdminSeasonController adminSeasonController;

    @BeforeEach
    void setUp() {
        seasonController = new SeasonController(seasonService);
        adminSeasonController = new AdminSeasonController(seasonService);
    }

    @Test
    @DisplayName("GET /api/seasons retourne la liste des saisons publiques")
    void getSeasons_returnsAllSeasons() {
        SeasonResponse resp = new SeasonResponse(1L, "Haute saison", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31),
                new BigDecimal("1.50"), 3, null, null, false);
        when(seasonService.getAllResponses()).thenReturn(List.of(resp));

        ResponseEntity<List<SeasonResponse>> response = seasonController.getSeasons(null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getNom()).isEqualTo("Haute saison");
    }

    @Test
    @DisplayName("GET /api/seasons avec agenceId retourne les saisons applicables")
    void getSeasons_withAgency_returnsApplicableSeasons() {
        SeasonResponse overrideResp = new SeasonResponse(2L, "Override Agence", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31),
                new BigDecimal("1.80"), 3, 99L, null, false);
        when(seasonService.getApplicable(99L, null)).thenReturn(List.of(overrideResp));

        ResponseEntity<List<SeasonResponse>> response = seasonController.getSeasons(99L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getNom()).isEqualTo("Override Agence");
        assertThat(response.getBody().get(0).getAgenceId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("POST /api/admin/seasons crée une nouvelle saison (201 Created)")
    void createSeason_returnsCreated() {
        SeasonRequest request = new SeasonRequest("Très haute Aïd", LocalDate.of(2026, 5, 25), LocalDate.of(2026, 6, 5),
                new BigDecimal("2.00"), 4, null, null, false);
        SeasonResponse response = new SeasonResponse(10L, "Très haute Aïd", LocalDate.of(2026, 5, 25), LocalDate.of(2026, 6, 5),
                new BigDecimal("2.00"), 4, null, null, false);

        when(seasonService.create(request)).thenReturn(response);

        ResponseEntity<SeasonResponse> result = adminSeasonController.createSeason(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().getId()).isEqualTo(10L);
        assertThat(result.getBody().getCoefficient()).isEqualByComparingTo("2.00");
    }

    @Test
    @DisplayName("PUT /api/admin/seasons/{id} met à jour la saison (200 OK)")
    void updateSeason_returnsUpdated() {
        SeasonRequest request = new SeasonRequest("Haute saison modifiée", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31),
                new BigDecimal("1.60"), 3, null, null, false);
        SeasonResponse response = new SeasonResponse(10L, "Haute saison modifiée", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31),
                new BigDecimal("1.60"), 3, null, null, false);

        when(seasonService.update(eq(10L), eq(request))).thenReturn(response);

        ResponseEntity<SeasonResponse> result = adminSeasonController.updateSeason(10L, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getCoefficient()).isEqualByComparingTo("1.60");
    }

    @Test
    @DisplayName("DELETE /api/admin/seasons/{id} supprime la saison (204 No Content)")
    void deleteSeason_returnsNoContent() {
        doNothing().when(seasonService).delete(10L);

        ResponseEntity<Void> result = adminSeasonController.deleteSeason(10L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(seasonService).delete(10L);
    }
}
