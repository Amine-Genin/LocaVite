package ma.locavite.reservation.service;

import ma.locavite.reservation.domain.Season;
import ma.locavite.reservation.dto.SeasonRequest;
import ma.locavite.reservation.dto.SeasonResponse;
import ma.locavite.reservation.event.SeasonChangedEvent;
import ma.locavite.reservation.exception.SeasonNotFoundException;
import ma.locavite.reservation.repository.SeasonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeasonServiceTest {

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private SeasonService seasonService;

    @BeforeEach
    void setUp() {
        seasonService = new SeasonService(seasonRepository, eventPublisher);
    }

    @Test
    @DisplayName("getAll retourne la liste ordonnée des saisons")
    void getAll_returnsOrderedSeasons() {
        Season s1 = new Season("Haute saison", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31), new BigDecimal("1.50"), 3);
        when(seasonRepository.findAllOrderedByPriority()).thenReturn(List.of(s1));

        List<Season> results = seasonService.getAll();
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getNom()).isEqualTo("Haute saison");
    }

    @Test
    @DisplayName("getById retourne la saison si elle existe")
    void getById_whenExists_returnsSeason() {
        Season s1 = new Season("Haute saison", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31), new BigDecimal("1.50"), 3);
        s1.setId(10L);
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(s1));

        Season result = seasonService.getById(10L);
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getNom()).isEqualTo("Haute saison");
    }

    @Test
    @DisplayName("getById lève SeasonNotFoundException si la saison n'existe pas")
    void getById_whenNotExists_throwsException() {
        when(seasonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> seasonService.getById(99L))
                .isInstanceOf(SeasonNotFoundException.class);
    }

    @Test
    @DisplayName("create valide les dates et publie SeasonChangedEvent")
    void create_validatesDatesAndPublishesEvent() {
        SeasonRequest request = new SeasonRequest(
                "Très haute Aïd",
                LocalDate.of(2026, 5, 25),
                LocalDate.of(2026, 6, 5),
                new BigDecimal("2.00"),
                4,
                null,
                null,
                false
        );

        when(seasonRepository.save(any(Season.class))).thenAnswer(invocation -> {
            Season s = invocation.getArgument(0);
            s.setId(100L);
            return s;
        });

        SeasonResponse response = seasonService.create(request);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getNom()).isEqualTo("Très haute Aïd");
        assertThat(response.getCoefficient()).isEqualByComparingTo("2.00");

        ArgumentCaptor<SeasonChangedEvent> eventCaptor = ArgumentCaptor.forClass(SeasonChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().action()).isEqualTo("CREATED");
        assertThat(eventCaptor.getValue().season().getId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("create rejette des dates incohérentes (début >= fin)")
    void create_invalidDates_throwsIllegalArgumentException() {
        SeasonRequest request = new SeasonRequest(
                "Saison invalide",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 7, 1),
                new BigDecimal("1.50"),
                1,
                null,
                null,
                false
        );

        assertThatThrownBy(() -> seasonService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("antérieure");
        verifyNoInteractions(seasonRepository);
    }

    @Test
    @DisplayName("update met à jour les champs et publie SeasonChangedEvent")
    void update_modifiesFieldsAndPublishesEvent() {
        Season existing = new Season("Ancien nom", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31), new BigDecimal("1.40"), 2);
        existing.setId(10L);

        when(seasonRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(seasonRepository.save(existing)).thenReturn(existing);

        SeasonRequest updateReq = new SeasonRequest(
                "Nouveau nom haute saison",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 31),
                new BigDecimal("1.50"),
                3,
                99L,
                null,
                true
        );

        SeasonResponse response = seasonService.update(10L, updateReq);

        assertThat(response.getNom()).isEqualTo("Nouveau nom haute saison");
        assertThat(response.getCoefficient()).isEqualByComparingTo("1.50");
        assertThat(response.getPriorite()).isEqualTo(3);
        assertThat(response.getAgenceId()).isEqualTo(99L);
        assertThat(response.getIsRecurring()).isTrue();

        verify(eventPublisher).publishEvent(any(SeasonChangedEvent.class));
    }

    @Test
    @DisplayName("delete supprime la saison et publie SeasonChangedEvent")
    void delete_removesSeasonAndPublishesEvent() {
        Season existing = new Season("Saison à supprimer", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31), new BigDecimal("1.50"), 1);
        existing.setId(10L);

        when(seasonRepository.findById(10L)).thenReturn(Optional.of(existing));

        seasonService.delete(10L);

        verify(seasonRepository).delete(existing);
        verify(eventPublisher).publishEvent(any(SeasonChangedEvent.class));
    }
}
