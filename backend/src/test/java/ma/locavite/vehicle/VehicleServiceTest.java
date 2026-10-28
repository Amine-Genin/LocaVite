package ma.locavite.vehicle;

import ma.locavite.catalog.domain.Brand;
import ma.locavite.catalog.domain.Category;
import ma.locavite.catalog.repository.BrandRepository;
import ma.locavite.catalog.repository.CategoryRepository;
import ma.locavite.vehicle.domain.*;
import ma.locavite.vehicle.dto.VehicleFilter;
import ma.locavite.vehicle.dto.VehicleRequest;
import ma.locavite.vehicle.dto.VehicleResponse;
import ma.locavite.vehicle.exception.MatriculeAlreadyExistsException;
import ma.locavite.vehicle.exception.VehicleNotFoundException;
import ma.locavite.vehicle.mapper.VehicleMapper;
import ma.locavite.vehicle.repository.VehicleRepository;
import ma.locavite.vehicle.service.CurrentUserService;
import ma.locavite.vehicle.service.VehicleService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    VehicleRepository repository;

    @Mock
    VehicleMapper mapper;

    @Mock
    CurrentUserService currentUser;

    @Mock
    BrandRepository brandRepository;

    @Mock
    CategoryRepository categoryRepository;

    @InjectMocks
    VehicleService service;

    @BeforeEach
    void setUpCatalogReferences() {
        Brand brand = new Brand();
        brand.setId(1L);
        brand.setName("Toyota");
        Category category = new Category();
        category.setId(1L);
        category.setName("BERLINE");
        lenient().when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        lenient().when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
    }

    // ==================== HELPERS ====================

    private VehicleRequest sampleRequest() {
        return new VehicleRequest(
                1L, "Corolla", 2022,
                1L, "Casablanca",
                new BigDecimal("350"),
                new BigDecimal("180000"),
                FuelType.DIESEL,
                TransmissionType.MANUELLE,
                5, "Très bon état",
                " 12345-a-1 ", null
        );
    }

    private VehicleRequest requestWithStatus(VehicleStatus status) {
        return new VehicleRequest(
                1L, "Corolla", 2022,
                1L, "Casablanca",
                new BigDecimal("350"),
                new BigDecimal("180000"),
                FuelType.DIESEL,
                TransmissionType.MANUELLE,
                5, "Très bon état",
                "12345-A-1", status
        );
    }

    private Vehicle sampleVehicle(
            Long id, Long agenceId, VehicleStatus status) {

        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setAgenceId(agenceId);
        Brand brand = new Brand();
        brand.setId(1L);
        brand.setName("Toyota");
        vehicle.setBrand(brand);
        vehicle.setModele("Corolla");
        vehicle.setAnnee(2022);
        Category category = new Category();
        category.setId(1L);
        category.setName("BERLINE");
        vehicle.setCategory(category);
        vehicle.setVille("Casablanca");
        vehicle.setPrixJour(new BigDecimal("350"));
        vehicle.setPrixVente(new BigDecimal("180000"));
        vehicle.setCarburant(FuelType.DIESEL);
        vehicle.setTransmission(TransmissionType.MANUELLE);
        vehicle.setPlaces(5);
        vehicle.setDescription("Très bon état");
        vehicle.setMatricule("12345-A-1");
        vehicle.setStatut(status);

        return vehicle;
    }

    private VehicleResponse sampleResponse() {
        return new VehicleResponse(
                1L, 1L, "Toyota", "Corolla", 2022,
                1L, "BERLINE", "Casablanca",
                new BigDecimal("350"),
                new BigDecimal("180000"),
                FuelType.DIESEL,
                TransmissionType.MANUELLE,
                5, "Très bon état", "12345-A-1",
                VehicleStatus.DRAFT, 7L,
                Instant.now(), Instant.now()
        );
    }

    // ==================== CREATE ====================

    @Test
    void create_shouldSaveVehicle() {
        VehicleRequest request = sampleRequest();

        when(currentUser.getCurrentUserId())
                .thenReturn(Optional.of(7L));
        when(repository.existsByMatriculeIgnoreCase("12345-A-1"))
                .thenReturn(false);
        when(mapper.toEntity(request))
                .thenReturn(new Vehicle());
        when(repository.saveAndFlush(any(Vehicle.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Vehicle.class)))
                .thenReturn(sampleResponse());

        VehicleResponse result = service.create(request);

        ArgumentCaptor<Vehicle> captor =
                ArgumentCaptor.forClass(Vehicle.class);

        verify(repository).saveAndFlush(captor.capture());

        Vehicle saved = captor.getValue();

        assertThat(saved.getAgenceId()).isEqualTo(7L);
        assertThat(saved.getStatut()).isEqualTo(VehicleStatus.DRAFT);
        assertThat(saved.getMatricule()).isEqualTo("12345-A-1");
        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void create_shouldUseProvidedStatus() {
        VehicleRequest request =
                requestWithStatus(VehicleStatus.PUBLISHED);
        Vehicle vehicle = new Vehicle();

        when(currentUser.getCurrentUserId())
                .thenReturn(Optional.of(7L));
        when(repository.existsByMatriculeIgnoreCase("12345-A-1"))
                .thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(vehicle);
        when(repository.saveAndFlush(any(Vehicle.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Vehicle.class)))
                .thenReturn(sampleResponse());

        service.create(request);

        assertThat(vehicle.getStatut())
                .isEqualTo(VehicleStatus.PUBLISHED);
        verify(repository).saveAndFlush(vehicle);
    }

    @Test
    void create_shouldThrowIfMatriculeExists() {
        when(currentUser.getCurrentUserId())
                .thenReturn(Optional.of(7L));
        when(repository.existsByMatriculeIgnoreCase("12345-A-1"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(sampleRequest()))
                .isInstanceOf(MatriculeAlreadyExistsException.class);

        verify(repository, never()).saveAndFlush(any());
        verify(mapper, never()).toEntity(any());
    }

    @Test
    void create_shouldRejectUnauthenticatedUser() {
        when(currentUser.getCurrentUserId())
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(sampleRequest()))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(repository, mapper);
    }

    @Test
    void create_shouldConvertDatabaseConflictToBusinessException() {
        when(currentUser.getCurrentUserId())
                .thenReturn(Optional.of(7L));
        when(repository.existsByMatriculeIgnoreCase("12345-A-1"))
                .thenReturn(false);
        when(mapper.toEntity(any(VehicleRequest.class)))
                .thenReturn(new Vehicle());
        when(repository.saveAndFlush(any(Vehicle.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Duplicate matricule"));

        assertThatThrownBy(() -> service.create(sampleRequest()))
                .isInstanceOf(MatriculeAlreadyExistsException.class);
    }

    // ==================== GET BY ID ====================

    @Test
    void getById_shouldReturnPublishedVehicle() {
        Vehicle vehicle =
                sampleVehicle(1L, 7L, VehicleStatus.PUBLISHED);

        when(repository.findById(1L))
                .thenReturn(Optional.of(vehicle));
        when(mapper.toResponse(vehicle))
                .thenReturn(sampleResponse());

        assertThat(service.getById(1L)).isNotNull();
        verify(mapper).toResponse(vehicle);
    }

    @Test
    void getById_shouldThrowWhenVehicleDoesNotExist() {
        when(repository.findById(100L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(100L))
                .isInstanceOf(VehicleNotFoundException.class);

        verify(mapper, never()).toResponse(any());
    }

    @Test
    void getById_shouldThrowWhenVehicleIsArchived() {
        when(repository.findById(1L))
                .thenReturn(Optional.of(
                        sampleVehicle(1L, 7L, VehicleStatus.ARCHIVED)));

        assertThatThrownBy(() -> service.getById(1L))
                .isInstanceOf(VehicleNotFoundException.class);
    }

    @Test
    void getById_shouldAllowAdminToSeeDraft() {
        Vehicle vehicle =
                sampleVehicle(1L, 99L, VehicleStatus.DRAFT);

        when(repository.findById(1L))
                .thenReturn(Optional.of(vehicle));
        when(currentUser.isAdmin()).thenReturn(true);
        when(mapper.toResponse(vehicle))
                .thenReturn(sampleResponse());

        assertThat(service.getById(1L)).isNotNull();
        verify(mapper).toResponse(vehicle);
    }

    @Test
    void getById_shouldAllowOwnerToSeeDraft() {
        Vehicle vehicle =
                sampleVehicle(1L, 7L, VehicleStatus.DRAFT);

        when(repository.findById(1L))
                .thenReturn(Optional.of(vehicle));
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.getCurrentUserId())
                .thenReturn(Optional.of(7L));
        when(mapper.toResponse(vehicle))
                .thenReturn(sampleResponse());

        assertThat(service.getById(1L)).isNotNull();
    }

    @Test
    void getById_shouldHideAnotherAgenciesDraft() {
        Vehicle vehicle =
                sampleVehicle(1L, 99L, VehicleStatus.DRAFT);

        when(repository.findById(1L))
                .thenReturn(Optional.of(vehicle));
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.getCurrentUserId())
                .thenReturn(Optional.of(7L));

        assertThatThrownBy(() -> service.getById(1L))
                .isInstanceOf(VehicleNotFoundException.class);
    }

    // ==================== SEARCH ====================

    @Test
    void search_shouldReturnPagedVehicles() {
        Pageable pageable = PageRequest.of(0, 10);
        VehicleFilter filter = mock(VehicleFilter.class);
        Vehicle vehicle =
                sampleVehicle(1L, 7L, VehicleStatus.PUBLISHED);

        Page<Vehicle> page =
                new PageImpl<>(List.of(vehicle), pageable, 1);

        when(repository.findAll(
                org.mockito.ArgumentMatchers
                        .<Specification<Vehicle>>any(),
                eq(pageable)))
                .thenReturn(page);
        when(mapper.toResponse(vehicle))
                .thenReturn(sampleResponse());

        Page<VehicleResponse> result =
                service.search(filter, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        verify(mapper).toResponse(vehicle);
    }

    @Test
    void search_shouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        VehicleFilter filter = mock(VehicleFilter.class);

        when(repository.findAll(
                org.mockito.ArgumentMatchers
                        .<Specification<Vehicle>>any(),
                eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<VehicleResponse> result =
                service.search(filter, pageable);

        assertThat(result).isEmpty();
        verify(mapper, never()).toResponse(any(Vehicle.class));
    }

    // ==================== UPDATE ====================

    @Test
    void update_shouldUpdateVehicleOwnedByCurrentAgency() {
        Vehicle vehicle =
                sampleVehicle(1L, 7L, VehicleStatus.PUBLISHED);
        VehicleRequest request = sampleRequest();

        when(repository.findById(1L))
                .thenReturn(Optional.of(vehicle));
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.getCurrentUserId())
                .thenReturn(Optional.of(7L));
        when(repository.saveAndFlush(vehicle))
                .thenReturn(vehicle);
        when(mapper.toResponse(vehicle))
                .thenReturn(sampleResponse());

        VehicleResponse result = service.update(1L, request);

        assertThat(result).isNotNull();
        verify(mapper).updateEntity(request, vehicle);
        verify(repository).saveAndFlush(vehicle);
    }

    @Test
    void update_shouldChangeStatusWhenRequestStatusIsProvided() {
        Vehicle vehicle =
                sampleVehicle(1L, 7L, VehicleStatus.DRAFT);
        VehicleRequest request =
                requestWithStatus(VehicleStatus.PUBLISHED);

        when(repository.findById(1L))
                .thenReturn(Optional.of(vehicle));
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.getCurrentUserId())
                .thenReturn(Optional.of(7L));
        when(repository.saveAndFlush(vehicle))
                .thenReturn(vehicle);
        when(mapper.toResponse(vehicle))
                .thenReturn(sampleResponse());

        service.update(1L, request);

        assertThat(vehicle.getStatut())
                .isEqualTo(VehicleStatus.PUBLISHED);
        verify(mapper).updateEntity(request, vehicle);
        verify(repository).saveAndFlush(vehicle);
    }

    @Test
    void update_shouldKeepExistingStatusWhenRequestStatusIsNull() {
        Vehicle vehicle =
                sampleVehicle(1L, 7L, VehicleStatus.PUBLISHED);
        VehicleRequest request = sampleRequest();

        when(repository.findById(1L))
                .thenReturn(Optional.of(vehicle));
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.getCurrentUserId())
                .thenReturn(Optional.of(7L));
        when(repository.saveAndFlush(vehicle))
                .thenReturn(vehicle);
        when(mapper.toResponse(vehicle))
                .thenReturn(sampleResponse());

        service.update(1L, request);

        assertThat(vehicle.getStatut())
                .isEqualTo(VehicleStatus.PUBLISHED);
        verify(repository).saveAndFlush(vehicle);
    }

    @Test
    void update_shouldAllowAdminToUpdateAnotherAgencyVehicle() {
        Vehicle vehicle =
                sampleVehicle(1L, 99L, VehicleStatus.PUBLISHED);
        VehicleRequest request = sampleRequest();

        when(repository.findById(1L))
                .thenReturn(Optional.of(vehicle));
        when(currentUser.isAdmin()).thenReturn(true);
        when(repository.saveAndFlush(vehicle))
                .thenReturn(vehicle);
        when(mapper.toResponse(vehicle))
                .thenReturn(sampleResponse());

        assertThat(service.update(1L, request)).isNotNull();
        verify(mapper).updateEntity(request, vehicle);
    }

    @Test
    void update_shouldThrowWhenVehicleDoesNotExist() {
        when(repository.findById(100L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(100L, sampleRequest()))
                .isInstanceOf(VehicleNotFoundException.class);
    }

    @Test
    void update_shouldThrowWhenVehicleIsArchived() {
        when(repository.findById(1L))
                .thenReturn(Optional.of(
                        sampleVehicle(1L, 7L, VehicleStatus.ARCHIVED)));

        assertThatThrownBy(() -> service.update(1L, sampleRequest()))
                .isInstanceOf(VehicleNotFoundException.class);
    }

    @Test
    void update_shouldRejectAnotherAgenciesVehicle() {
        when(repository.findById(1L))
                .thenReturn(Optional.of(
                        sampleVehicle(1L, 99L, VehicleStatus.PUBLISHED)));
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.getCurrentUserId())
                .thenReturn(Optional.of(7L));

        assertThatThrownBy(() -> service.update(1L, sampleRequest()))
                .isInstanceOf(AccessDeniedException.class);

        verify(mapper, never()).updateEntity(any(), any());
    }

    @Test
    void update_shouldRejectDuplicateMatricule() {
        Vehicle vehicle =
                sampleVehicle(1L, 7L, VehicleStatus.PUBLISHED);
        vehicle.setMatricule("OLD-A-1");

        when(repository.findById(1L))
                .thenReturn(Optional.of(vehicle));
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.getCurrentUserId())
                .thenReturn(Optional.of(7L));
        when(repository.existsByMatriculeIgnoreCase("12345-A-1"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, sampleRequest()))
                .isInstanceOf(MatriculeAlreadyExistsException.class);

        verify(mapper, never()).updateEntity(any(), any());
    }

    @Test
    void update_shouldKeepSameMatriculeWithoutDuplicateLookup() {
        Vehicle vehicle =
                sampleVehicle(1L, 7L, VehicleStatus.PUBLISHED);

        when(repository.findById(1L))
                .thenReturn(Optional.of(vehicle));
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.getCurrentUserId())
                .thenReturn(Optional.of(7L));
        when(repository.saveAndFlush(vehicle))
                .thenReturn(vehicle);
        when(mapper.toResponse(vehicle))
                .thenReturn(sampleResponse());

        service.update(1L, sampleRequest());

        verify(repository, never())
                .existsByMatriculeIgnoreCase(any());
    }

    @Test
    void update_shouldConvertDatabaseConflictToBusinessException() {
        Vehicle vehicle =
                sampleVehicle(1L, 7L, VehicleStatus.PUBLISHED);

        when(repository.findById(1L))
                .thenReturn(Optional.of(vehicle));
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.getCurrentUserId())
                .thenReturn(Optional.of(7L));
        when(repository.saveAndFlush(vehicle))
                .thenThrow(new DataIntegrityViolationException(
                        "Duplicate matricule"));

        assertThatThrownBy(() -> service.update(1L, sampleRequest()))
                .isInstanceOf(MatriculeAlreadyExistsException.class);
    }

    // ==================== DELETE ====================

    @Test
    void delete_shouldArchiveVehicle() {
        Vehicle vehicle =
                sampleVehicle(1L, 7L, VehicleStatus.PUBLISHED);

        when(repository.findById(1L))
                .thenReturn(Optional.of(vehicle));
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.getCurrentUserId())
                .thenReturn(Optional.of(7L));

        service.delete(1L);

        assertThat(vehicle.getStatut())
                .isEqualTo(VehicleStatus.ARCHIVED);
        verify(repository).save(vehicle);
    }

    @Test
    void delete_shouldAllowAdminToArchiveAnotherAgenciesVehicle() {
        Vehicle vehicle =
                sampleVehicle(1L, 99L, VehicleStatus.PUBLISHED);

        when(repository.findById(1L))
                .thenReturn(Optional.of(vehicle));
        when(currentUser.isAdmin()).thenReturn(true);

        service.delete(1L);

        assertThat(vehicle.getStatut())
                .isEqualTo(VehicleStatus.ARCHIVED);
        verify(repository).save(vehicle);
    }

    @Test
    void delete_shouldThrowWhenVehicleDoesNotExist() {
        when(repository.findById(100L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(100L))
                .isInstanceOf(VehicleNotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void delete_shouldThrowWhenVehicleIsArchived() {
        when(repository.findById(1L))
                .thenReturn(Optional.of(
                        sampleVehicle(1L, 7L, VehicleStatus.ARCHIVED)));

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(VehicleNotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void delete_shouldRejectAnotherAgenciesVehicle() {
        when(repository.findById(1L))
                .thenReturn(Optional.of(
                        sampleVehicle(1L, 99L, VehicleStatus.PUBLISHED)));
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.getCurrentUserId())
                .thenReturn(Optional.of(7L));

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(AccessDeniedException.class);

        verify(repository, never()).save(any());
    }
}