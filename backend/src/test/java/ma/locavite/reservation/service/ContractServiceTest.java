package ma.locavite.reservation.service;

import ma.locavite.auth.domain.User;
import ma.locavite.auth.repository.UserRepository;
import ma.locavite.catalog.domain.Brand;
import ma.locavite.catalog.domain.Category;
import ma.locavite.document.repository.DocumentRepository;
import ma.locavite.document.service.MinioService;
import ma.locavite.reservation.config.ContractProperties;
import ma.locavite.reservation.domain.ContractLanguage;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationContract;
import ma.locavite.reservation.domain.ReservationOption;
import ma.locavite.reservation.domain.ReservationStatus;
import ma.locavite.reservation.dto.PriceQuoteResponse;
import ma.locavite.reservation.repository.ReservationContractRepository;
import ma.locavite.reservation.repository.ReservationRepository;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.repository.VehicleRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private ReservationContractRepository contractRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private PricingService pricingService;
    @Mock
    private MinioService minioService;
    @Mock
    private org.springframework.mail.javamail.JavaMailSender mailSender;

    private ContractService contractService;
    private Reservation reservation;
    private User user;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        ContractProperties properties = new ContractProperties();
        properties.setPublicBaseUrl("https://locavite.example");
        contractService = new ContractService(
                reservationRepository, contractRepository, userRepository, vehicleRepository,
                documentRepository, pricingService, minioService, mailSender, properties,
                new PdfFontPathResolver(properties));

        reservation = new Reservation();
        reservation.setId(42L);
        reservation.setCode("LV-ABC123");
        reservation.setUserId(7L);
        reservation.setVehicleId(12L);
        reservation.setDateDebut(LocalDate.of(2026, 12, 1));
        reservation.setDateFin(LocalDate.of(2026, 12, 4));
        reservation.setLieuPriseEnCharge("Aéroport Casablanca");
        reservation.setLieuRetour("Gare Rabat");
        reservation.setPrixTotal(new BigDecimal("4620.00"));
        reservation.setCaution(new BigDecimal("3000.00"));
        reservation.setPrixSousTotal(new BigDecimal("1200.00"));
        reservation.setPrixOptions(new BigDecimal("150.00"));
        reservation.setPrixRemise(BigDecimal.ZERO.setScale(2));
        reservation.setPrixTva(new BigDecimal("270.00"));
        reservation.setOptions(Set.of(ReservationOption.GPS));
        reservation.setStatut(ReservationStatus.CONFIRMED);

        user = new User();
        user.setId(7L);
        user.setNom("Amrani");
        user.setPrenom("Salma");
        user.setEmail("salma@example.com");

        Brand brand = new Brand();
        brand.setName("Toyota");
        Category category = new Category();
        category.setCaution(new BigDecimal("3000.00"));
        vehicle = new Vehicle();
        vehicle.setId(12L);
        vehicle.setBrand(brand);
        vehicle.setCategory(category);
        vehicle.setModele("RAV4");
        vehicle.setMatricule("12345-A-6");
        vehicle.setPrixJour(new BigDecimal("400.00"));

    }

    @Test
    @DisplayName("Generates a parseable PDF/A contract with the reservation details")
    void generate_createsValidPdfWithContractContent() throws IOException {
        when(contractRepository.findByReservationIdAndLanguage(42L, ContractLanguage.FR))
                .thenReturn(Optional.empty());
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(vehicleRepository.findById(12L)).thenReturn(Optional.of(vehicle));
        when(documentRepository.findByUserIdOrderByUploadedAtDesc(7L)).thenReturn(List.of());
        when(pricingService.calculate(eq(vehicle), any(), any(), any()))
                .thenReturn(new PriceQuoteResponse(
                        new BigDecimal("1200.00"), new BigDecimal("150.00"), BigDecimal.ZERO.setScale(2),
                        new BigDecimal("3000.00"), new BigDecimal("270.00"), new BigDecimal("4620.00"), 3));
        when(contractRepository.save(any(ReservationContract.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        contractService.generate(reservation, ContractLanguage.FR);

        ArgumentCaptor<byte[]> pdfCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(minioService).putContract(eq("42/fr/contract-LV-ABC123.pdf"), pdfCaptor.capture());
        byte[] pdfBytes = pdfCaptor.getValue();
        assertThat(pdfBytes).isNotEmpty();
        assertThat(new String(pdfBytes, 0, 5)).isEqualTo("%PDF-");

        try (org.apache.pdfbox.pdmodel.PDDocument pdf = Loader.loadPDF(pdfBytes)) {
            assertThat(pdf.getNumberOfPages()).isPositive();
            assertThat(pdf.getPage(0).getMediaBox().getWidth()).isEqualTo(595.0f);
            String text = new PDFTextStripper().getText(pdf);
            assertThat(text).contains("LocaVite", "LV-ABC123", "Salma Amrani",
                    "Toyota", "RAV4", "12345-A-6", "Caution", "TVA", "4620.00 MAD",
                    "PROVISOIRE");
        }

        ArgumentCaptor<ReservationContract> contractCaptor = ArgumentCaptor.forClass(ReservationContract.class);
        verify(contractRepository).save(contractCaptor.capture());
        assertThat(contractCaptor.getValue().getAccessToken()).isNotBlank();
    }

    @Test
    @DisplayName("Uses the signed URL only for the reservation owner")
    void getDownloadUrl_requiresReservationOwner() {
        when(reservationRepository.findById(42L)).thenReturn(Optional.of(reservation));
        ReservationContract contract = new ReservationContract();
        contract.setObjectPath("42/fr/contract-LV-ABC123.pdf");
        when(contractRepository.findByReservationIdAndLanguage(42L, ContractLanguage.FR))
                .thenReturn(Optional.of(contract));
        when(minioService.getContractPresignedUrl(anyString())).thenReturn("https://minio.example/signed");

        assertThat(contractService.getDownloadUrl(42L, 7L, ContractLanguage.FR))
                .isEqualTo("https://minio.example/signed");
        assertThatThrownBy(() -> contractService.getDownloadUrl(42L, 8L, ContractLanguage.FR))
                .isInstanceOf(AccessDeniedException.class);
        verify(minioService, times(1)).getContractPresignedUrl(anyString());
    }

    @Test
    @DisplayName("Test : nouvelle version du contrat générée lors d'une prolongation")
    void regenerate_createsNewContractVersionWithUpdatedDatesAndPrice() throws IOException {
        ReservationContract existingContract = new ReservationContract();
        existingContract.setId(1L);
        existingContract.setReservation(reservation);
        existingContract.setLanguage(ContractLanguage.FR);
        existingContract.setObjectPath("42/fr/contract-LV-ABC123.pdf");
        existingContract.setAccessToken("old-token");
        existingContract.setVersion(1);

        reservation.setDateFin(LocalDate.of(2026, 12, 6));
        reservation.setPrixSousTotal(new BigDecimal("2000.00"));
        reservation.setPrixOptions(new BigDecimal("250.00"));
        reservation.setPrixTva(new BigDecimal("450.00"));
        reservation.setPrixTotal(new BigDecimal("5700.00"));

        when(contractRepository.findByReservationIdAndLanguage(42L, ContractLanguage.FR))
                .thenReturn(Optional.of(existingContract));
        when(contractRepository.findByReservationId(42L))
                .thenReturn(List.of(existingContract));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(vehicleRepository.findById(12L)).thenReturn(Optional.of(vehicle));
        when(documentRepository.findByUserIdOrderByUploadedAtDesc(7L)).thenReturn(List.of());
        when(pricingService.calculate(eq(vehicle), any(), any(), any()))
                .thenReturn(new PriceQuoteResponse(
                        new BigDecimal("2000.00"), new BigDecimal("250.00"), BigDecimal.ZERO.setScale(2),
                        new BigDecimal("3000.00"), new BigDecimal("450.00"), new BigDecimal("5700.00"), 5));
        when(contractRepository.save(any(ReservationContract.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationContract regenerated = contractService.regenerate(reservation, ContractLanguage.FR);

        assertThat(regenerated.getVersion()).isEqualTo(2);
        assertThat(regenerated.getObjectPath()).isEqualTo("42/fr/contract-LV-ABC123-v2.pdf");
        assertThat(regenerated.getAccessToken()).isNotEqualTo("old-token");

        ArgumentCaptor<byte[]> pdfCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(minioService).putContract(eq("42/fr/contract-LV-ABC123-v2.pdf"), pdfCaptor.capture());

        try (org.apache.pdfbox.pdmodel.PDDocument pdf = Loader.loadPDF(pdfCaptor.getValue())) {
            String text = new PDFTextStripper().getText(pdf);
            assertThat(text).contains("06/12/2026", "5700.00 MAD");
        }
    }
}
