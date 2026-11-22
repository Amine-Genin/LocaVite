package ma.locavite.sale.service;

import ma.locavite.sale.domain.SaleListing;
import ma.locavite.sale.domain.SaleStatus;
import ma.locavite.sale.domain.SaleTaxRegime;
import ma.locavite.sale.domain.VehicleCondition;
import ma.locavite.sale.dto.SaleListingFilter;
import ma.locavite.sale.dto.SaleListingRequest;
import ma.locavite.sale.dto.SaleListingResponse;
import ma.locavite.sale.exception.SaleListingNotFoundException;
import ma.locavite.sale.exception.VinAlreadyExistsException;
import ma.locavite.sale.mapper.SaleListingMapper;
import ma.locavite.sale.repository.SaleListingRepository;
import ma.locavite.vehicle.domain.FuelType;
import ma.locavite.vehicle.domain.TransmissionType;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.exception.VehicleNotFoundException;
import ma.locavite.vehicle.repository.VehicleRepository;
import ma.locavite.vehicle.repository.VehiclePhotoRepository;
import ma.locavite.vehicle.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaleListingServiceTest {

    private static final Long AGENCY_ID = 10L;
    private static final Long OTHER_AGENCY_ID = 99L;
    private static final Long VEHICLE_ID = 5L;

    @Mock
    private SaleListingRepository repository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private VehiclePhotoRepository vehiclePhotoRepository;

    @Mock
    private CurrentUserService currentUser;

    private final SaleListingMapper mapper = Mappers.getMapper(SaleListingMapper.class);

    private SaleListingService service;

    private Vehicle sampleVehicle;

    @BeforeEach
    void setUp() {
        service = new SaleListingService(repository, vehicleRepository, mapper, currentUser);
        ReflectionTestUtils.setField(mapper, "vehiclePhotoRepository", vehiclePhotoRepository);

        sampleVehicle = new Vehicle();
        sampleVehicle.setId(VEHICLE_ID);
        sampleVehicle.setAgenceId(AGENCY_ID);
        sampleVehicle.setModele("Golf 8");
        sampleVehicle.setCarburant(FuelType.DIESEL);
        sampleVehicle.setTransmission(TransmissionType.MANUELLE);
    }

    private SaleListingRequest buildRequest(String vin) {
        return new SaleListingRequest(
                VEHICLE_ID,
                "Golf 8 R-Line 2.0 TDI",
                "Véhicule en parfait état de première main",
                new BigDecimal("280000.00"),
                true,
                VehicleCondition.USED,
                35000,
                true,
                LocalDate.of(2022, 6, 15),
                vin,
                1,
                SaleStatus.DRAFT
        );
    }

    private SaleListing buildListing(Long id, Long agenceId, SaleStatus status) {
        SaleListing listing = new SaleListing();
        listing.setId(id);
        listing.setCode("SL-ABC123");
        listing.setVehicle(sampleVehicle);
        listing.setAgenceId(agenceId);
        listing.setTitre("Golf 8 R-Line");
        listing.setDescription("Description");
        listing.setPrixVente(new BigDecimal("280000.00"));
        listing.setPrixNego(false);
        listing.setEtat(VehicleCondition.USED);
        listing.setKilometrage(35000);
        listing.setPremierProprietaire(true);
        listing.setNumeroChassis("WVWZZZCDZPW000001");
        listing.setStatut(status);
        listing.setDeleted(false);
        return listing;
    }

    @Nested
    @DisplayName("Tests de création (create)")
    class CreateTests {

        @Test
        @DisplayName("Création réussie par l'agence propriétaire du véhicule")
        void create_success() {
            when(currentUser.getCurrentUserId()).thenReturn(Optional.of(AGENCY_ID));
            when(currentUser.isAdmin()).thenReturn(false);
            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(sampleVehicle));
            when(repository.existsByNumeroChassisIgnoreCaseAndDeletedFalse(anyString())).thenReturn(false);
            when(repository.existsByCode(anyString())).thenReturn(false);
            when(repository.save(any(SaleListing.class))).thenAnswer(inv -> {
                SaleListing entity = inv.getArgument(0);
                entity.setId(100L);
                return entity;
            });

            SaleListingRequest request = buildRequest("wvwzzzcdzpw000001");
            SaleListingResponse response = service.create(request);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(100L);
            assertThat(response.code()).startsWith("SL-");
            assertThat(response.agenceId()).isEqualTo(AGENCY_ID);
            assertThat(response.vehicleId()).isEqualTo(VEHICLE_ID);
            assertThat(response.carburant()).isEqualTo(FuelType.DIESEL.name());
            assertThat(response.transmission()).isEqualTo(TransmissionType.MANUELLE.name());
            assertThat(response.statut()).isEqualTo(SaleStatus.DRAFT);
            assertThat(response.numeroChassis()).isEqualTo("WVWZZZCDZPW000001");
            assertThat(response.garantieConstructeur()).isFalse();
            assertThat(response.regimeTva()).isEqualTo(SaleTaxRegime.MARGIN);

            ArgumentCaptor<SaleListing> captor = ArgumentCaptor.forClass(SaleListing.class);
            verify(repository).save(captor.capture());
            SaleListing saved = captor.getValue();
            assertThat(saved.getDeleted()).isFalse();
            assertThat(saved.getPrixNego()).isTrue();
            assertThat(saved.getPremierProprietaire()).isTrue();
        }

        @Test
        @DisplayName("Création d'une occasion sans date de mise en circulation échoue")
        void create_usedWithoutRegistrationDate_throwsBadRequest() {
            SaleListingRequest request = new SaleListingRequest(
                    VEHICLE_ID,
                    "Golf 8 R-Line 2.0 TDI",
                    "Description",
                    new BigDecimal("280000.00"),
                    true,
                    VehicleCondition.USED,
                    35000,
                    true,
                    null,
                    null,
                    1,
                    SaleStatus.DRAFT
            );

            assertThatThrownBy(() -> service.create(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("date de mise en circulation");
            verifyNoInteractions(currentUser, vehicleRepository, repository);
        }

        @Test
        @DisplayName("Création échoue si l'utilisateur n'est pas authentifié")
        void create_unauthenticated_throwsAccessDenied() {
            when(currentUser.getCurrentUserId()).thenReturn(Optional.empty());

            SaleListingRequest request = buildRequest(null);
            assertThatThrownBy(() -> service.create(request))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("non authentifié");
        }

        @Test
        @DisplayName("Création échoue si le véhicule n'existe pas")
        void create_vehicleNotFound_throwsException() {
            when(currentUser.getCurrentUserId()).thenReturn(Optional.of(AGENCY_ID));
            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.empty());

            SaleListingRequest request = buildRequest(null);
            assertThatThrownBy(() -> service.create(request))
                    .isInstanceOf(VehicleNotFoundException.class);
        }

        @Test
        @DisplayName("Création échoue si le véhicule appartient à une autre agence")
        void create_vehicleBelongsToOtherAgency_throwsAccessDenied() {
            when(currentUser.getCurrentUserId()).thenReturn(Optional.of(OTHER_AGENCY_ID));
            when(currentUser.isAdmin()).thenReturn(false);
            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(sampleVehicle));

            SaleListingRequest request = buildRequest(null);
            assertThatThrownBy(() -> service.create(request))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("propres véhicules");
        }

        @Test
        @DisplayName("Création échoue si le VIN existe déjà")
        void create_duplicateVin_throwsConflict() {
            when(currentUser.getCurrentUserId()).thenReturn(Optional.of(AGENCY_ID));
            when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(sampleVehicle));
            when(repository.existsByNumeroChassisIgnoreCaseAndDeletedFalse("WVWZZZCDZPW000001")).thenReturn(true);

            SaleListingRequest request = buildRequest("WVWZZZCDZPW000001");
            assertThatThrownBy(() -> service.create(request))
                    .isInstanceOf(VinAlreadyExistsException.class)
                    .hasMessageContaining("WVWZZZCDZPW000001");
        }
    }

    @Nested
    @DisplayName("Tests de consultation (getById)")
    class GetByIdTests {

        @Test
        @DisplayName("Consultation publique d'une annonce publiée")
        void getById_published_returnsResponse() {
            SaleListing listing = buildListing(1L, AGENCY_ID, SaleStatus.PUBLISHED);
            when(repository.findById(1L)).thenReturn(Optional.of(listing));

            SaleListingResponse response = service.getById(1L);
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.statut()).isEqualTo(SaleStatus.PUBLISHED);
        }

        @Test
        @DisplayName("Consultation d'un brouillon par un tiers retourne 404")
        void getById_draftByOther_throwsNotFound() {
            SaleListing listing = buildListing(1L, AGENCY_ID, SaleStatus.DRAFT);
            when(repository.findById(1L)).thenReturn(Optional.of(listing));
            when(currentUser.isAdmin()).thenReturn(false);
            when(currentUser.getCurrentUserId()).thenReturn(Optional.of(OTHER_AGENCY_ID));

            assertThatThrownBy(() -> service.getById(1L))
                    .isInstanceOf(SaleListingNotFoundException.class);
        }

        @Test
        @DisplayName("Consultation d'un brouillon par le propriétaire réussit")
        void getById_draftByOwner_succeeds() {
            SaleListing listing = buildListing(1L, AGENCY_ID, SaleStatus.DRAFT);
            when(repository.findById(1L)).thenReturn(Optional.of(listing));
            when(currentUser.isAdmin()).thenReturn(false);
            when(currentUser.getCurrentUserId()).thenReturn(Optional.of(AGENCY_ID));

            SaleListingResponse response = service.getById(1L);
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Consultation d'une annonce archivée retourne 404")
        void getById_archived_throwsNotFound() {
            SaleListing listing = buildListing(1L, AGENCY_ID, SaleStatus.ARCHIVED);
            when(repository.findById(1L)).thenReturn(Optional.of(listing));

            assertThatThrownBy(() -> service.getById(1L))
                    .isInstanceOf(SaleListingNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Tests de modification (update)")
    class UpdateTests {

        @Test
        @DisplayName("Mise à jour réussie par l'agence propriétaire")
        void update_success() {
            SaleListing listing = buildListing(1L, AGENCY_ID, SaleStatus.DRAFT);
            when(repository.findById(1L)).thenReturn(Optional.of(listing));
            when(currentUser.isAdmin()).thenReturn(false);
            when(currentUser.getCurrentUserId()).thenReturn(Optional.of(AGENCY_ID));
            when(repository.existsByNumeroChassisIgnoreCaseAndIdNotAndDeletedFalse(anyString(), eq(1L))).thenReturn(false);
            when(repository.save(any(SaleListing.class))).thenAnswer(inv -> inv.getArgument(0));

            SaleListingRequest request = new SaleListingRequest(
                    VEHICLE_ID,
                    "Golf 8 Modifiée",
                    "Description mise à jour",
                    new BigDecimal("290000.00"),
                    true,
                    VehicleCondition.USED,
                    40000,
                    true,
                    LocalDate.of(2022, 6, 15),
                    "WVWZZZCDZPW000002",
                    1,
                    SaleStatus.DRAFT
            );

            SaleListingResponse response = service.update(1L, request);
            assertThat(response.titre()).isEqualTo("Golf 8 Modifiée");
            assertThat(response.prixVente()).isEqualByComparingTo(new BigDecimal("290000.00"));
            assertThat(response.numeroChassis()).isEqualTo("WVWZZZCDZPW000002");
        }

        @Test
        @DisplayName("Mise à jour par une autre agence lève AccessDeniedException")
        void update_otherAgency_throwsAccessDenied() {
            SaleListing listing = buildListing(1L, AGENCY_ID, SaleStatus.DRAFT);
            when(repository.findById(1L)).thenReturn(Optional.of(listing));
            when(currentUser.isAdmin()).thenReturn(false);
            when(currentUser.getCurrentUserId()).thenReturn(Optional.of(OTHER_AGENCY_ID));

            SaleListingRequest request = buildRequest(null);
            assertThatThrownBy(() -> service.update(1L, request))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("propriétaire");
        }

        @Test
        @DisplayName("Mise à jour avec VIN déjà existant lève VinAlreadyExistsException")
        void update_duplicateVin_throwsConflict() {
            SaleListing listing = buildListing(1L, AGENCY_ID, SaleStatus.DRAFT);
            when(repository.findById(1L)).thenReturn(Optional.of(listing));
            when(currentUser.isAdmin()).thenReturn(false);
            when(currentUser.getCurrentUserId()).thenReturn(Optional.of(AGENCY_ID));
            when(repository.existsByNumeroChassisIgnoreCaseAndIdNotAndDeletedFalse("WVWZZZCDZPW000099", 1L)).thenReturn(true);

            SaleListingRequest request = buildRequest("WVWZZZCDZPW000099");
            assertThatThrownBy(() -> service.update(1L, request))
                    .isInstanceOf(VinAlreadyExistsException.class);
        }
    }

    @Nested
    @DisplayName("Tests de suppression (delete)")
    class DeleteTests {

        @Test
        @DisplayName("Suppression douce (soft delete) par l'agence propriétaire")
        void delete_asOwner_softDeletes() {
            SaleListing listing = buildListing(1L, AGENCY_ID, SaleStatus.PUBLISHED);
            when(repository.findById(1L)).thenReturn(Optional.of(listing));
            when(currentUser.isAdmin()).thenReturn(false);
            when(currentUser.getCurrentUserId()).thenReturn(Optional.of(AGENCY_ID));

            service.delete(1L);

            ArgumentCaptor<SaleListing> captor = ArgumentCaptor.forClass(SaleListing.class);
            verify(repository).save(captor.capture());
            SaleListing saved = captor.getValue();
            assertThat(saved.getDeleted()).isTrue();
            assertThat(saved.getStatut()).isEqualTo(SaleStatus.ARCHIVED);
        }

        @Test
        @DisplayName("Suppression par une autre agence refusée")
        void delete_asOtherAgency_throwsAccessDenied() {
            SaleListing listing = buildListing(1L, AGENCY_ID, SaleStatus.PUBLISHED);
            when(repository.findById(1L)).thenReturn(Optional.of(listing));
            when(currentUser.isAdmin()).thenReturn(false);
            when(currentUser.getCurrentUserId()).thenReturn(Optional.of(OTHER_AGENCY_ID));

            assertThatThrownBy(() -> service.delete(1L))
                    .isInstanceOf(AccessDeniedException.class);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tests de publication (publish)")
    class PublishTests {

        @Test
        @DisplayName("Publication par l'agence propriétaire passe le statut à PUBLISHED")
        void publish_asOwner_setsPublished() {
            SaleListing listing = buildListing(1L, AGENCY_ID, SaleStatus.DRAFT);
            when(repository.findById(1L)).thenReturn(Optional.of(listing));
            when(currentUser.isAdmin()).thenReturn(false);
            when(currentUser.getCurrentUserId()).thenReturn(Optional.of(AGENCY_ID));
            when(repository.save(any(SaleListing.class))).thenAnswer(inv -> inv.getArgument(0));

            SaleListingResponse response = service.publish(1L);
            assertThat(response.statut()).isEqualTo(SaleStatus.PUBLISHED);
            assertThat(response.publishedAt()).isNotNull();
        }

        @Test
        @DisplayName("Publication par une autre agence refusée")
        void publish_asOtherAgency_throwsAccessDenied() {
            SaleListing listing = buildListing(1L, AGENCY_ID, SaleStatus.DRAFT);
            when(repository.findById(1L)).thenReturn(Optional.of(listing));
            when(currentUser.isAdmin()).thenReturn(false);
            when(currentUser.getCurrentUserId()).thenReturn(Optional.of(OTHER_AGENCY_ID));

            assertThatThrownBy(() -> service.publish(1L))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("Tests de vente (markSold)")
    class MarkSoldTests {

        @Test
        @DisplayName("Marquage vendu par l'agence propriétaire passe le statut à SOLD avec soldAt")
        void markSold_asOwner_setsSold() {
            SaleListing listing = buildListing(1L, AGENCY_ID, SaleStatus.PUBLISHED);
            when(repository.findById(1L)).thenReturn(Optional.of(listing));
            when(currentUser.isAdmin()).thenReturn(false);
            when(currentUser.getCurrentUserId()).thenReturn(Optional.of(AGENCY_ID));
            when(repository.save(any(SaleListing.class))).thenAnswer(inv -> inv.getArgument(0));

            SaleListingResponse response = service.markSold(1L);
            assertThat(response.statut()).isEqualTo(SaleStatus.SOLD);
            assertThat(response.soldAt()).isNotNull();
        }

        @Test
        @DisplayName("Marquage vendu par une autre agence refusée")
        void markSold_asOtherAgency_throwsAccessDenied() {
            SaleListing listing = buildListing(1L, AGENCY_ID, SaleStatus.PUBLISHED);
            when(repository.findById(1L)).thenReturn(Optional.of(listing));
            when(currentUser.isAdmin()).thenReturn(false);
            when(currentUser.getCurrentUserId()).thenReturn(Optional.of(OTHER_AGENCY_ID));

            assertThatThrownBy(() -> service.markSold(1L))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("Tests de recherche (search)")
    class SearchTests {

        @Test
        @DisplayName("Recherche publique filtre uniquement les annonces publiées")
        void search_public_onlyPublished() {
            when(currentUser.isAdmin()).thenReturn(false);

            SaleListing listing = buildListing(1L, AGENCY_ID, SaleStatus.PUBLISHED);
            Page<SaleListing> page = new PageImpl<>(List.of(listing));
            when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

            SaleListingFilter filter = new SaleListingFilter("Golf", VehicleCondition.USED, null, null, null, null, null, null);
            Page<SaleListingResponse> result = service.search(filter, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).titre()).isEqualTo("Golf 8 R-Line");
        }

        @Test
        @DisplayName("Recherche par agence propriétaire inclut ses brouillons")
        void search_byOwnerAgency_includesDrafts() {
            when(currentUser.isAdmin()).thenReturn(false);
            when(currentUser.getCurrentUserId()).thenReturn(Optional.of(AGENCY_ID));

            SaleListing listing = buildListing(1L, AGENCY_ID, SaleStatus.DRAFT);
            Page<SaleListing> page = new PageImpl<>(List.of(listing));
            when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

            SaleListingFilter filter = new SaleListingFilter(null, null, null, null, null, null, null, AGENCY_ID);
            Page<SaleListingResponse> result = service.search(filter, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).statut()).isEqualTo(SaleStatus.DRAFT);
        }
    }
}
