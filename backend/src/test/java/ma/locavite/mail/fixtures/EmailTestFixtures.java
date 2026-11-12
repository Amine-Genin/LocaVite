package ma.locavite.mail.fixtures;

import ma.locavite.auth.domain.User;
import ma.locavite.catalog.domain.Brand;
import ma.locavite.catalog.domain.Category;
import ma.locavite.mail.domain.EmailLog;
import ma.locavite.reservation.domain.ContractLanguage;
import ma.locavite.reservation.domain.Reservation;
import ma.locavite.reservation.domain.ReservationContract;
import ma.locavite.reservation.domain.ReservationStatus;
import ma.locavite.vehicle.domain.FuelType;
import ma.locavite.vehicle.domain.TransmissionType;
import ma.locavite.vehicle.domain.Vehicle;
import ma.locavite.vehicle.domain.VehicleStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class EmailTestFixtures {

    private EmailTestFixtures() {
    }

    public static User createClient() {
        return createClient(10L, "karim.benali@example.com", "Benali", "Karim");
    }

    public static User createClient(Long id, String email, String nom, String prenom) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setNom(nom);
        user.setPrenom(prenom);
        user.setTelephone("+212600112233");
        user.setPassword("hashedpassword123");
        return user;
    }

    public static Brand createBrand(String name) {
        Brand brand = new Brand();
        brand.setName(name);
        return brand;
    }

    public static Category createCategory(String name, BigDecimal caution) {
        Category category = new Category();
        category.setName(name);
        category.setCaution(caution);
        return category;
    }

    public static Vehicle createVehicle(Brand brand, Category category) {
        return createVehicle(20L, brand, category, "12345-A-6", 1L, new BigDecimal("400.00"));
    }

    public static Vehicle createVehicle(Long id, Brand brand, Category category, String matricule, Long agenceId, BigDecimal prixJour) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setBrand(brand);
        vehicle.setCategory(category);
        vehicle.setModele("RAV4");
        vehicle.setMatricule(matricule);
        vehicle.setPrixJour(prixJour);
        vehicle.setPrixVente(new BigDecimal("250000.00"));
        vehicle.setAnnee(2023);
        vehicle.setPlaces(5);
        vehicle.setDescription("Véhicule de test");
        vehicle.setVille("Casablanca");
        vehicle.setCarburant(FuelType.ESSENCE);
        vehicle.setTransmission(TransmissionType.MANUELLE);
        vehicle.setStatut(VehicleStatus.PUBLISHED);
        vehicle.setAgenceId(agenceId);
        return vehicle;
    }

    public static Reservation createReservation(User client, Vehicle vehicle) {
        return createReservation(100L, "LV-CONFIRM-123", client, vehicle);
    }

    public static Reservation createReservation(Long id, String code, User client, Vehicle vehicle) {
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setCode(code);
        reservation.setUserId(client != null ? client.getId() : null);
        reservation.setVehicleId(vehicle != null ? vehicle.getId() : null);
        reservation.setDateDebut(LocalDate.of(2026, 12, 1));
        reservation.setDateFin(LocalDate.of(2026, 12, 5));
        reservation.setLieuPriseEnCharge("Aéroport Casablanca");
        reservation.setLieuRetour("Gare Rabat");
        reservation.setPrixTotal(new BigDecimal("4500.00"));
        reservation.setCaution(new BigDecimal("3000.00"));
        reservation.setStatut(ReservationStatus.CONFIRMED);
        return reservation;
    }

    public static ReservationContract createContract(Reservation reservation, ContractLanguage language) {
        ReservationContract contract = new ReservationContract();
        contract.setId(1L);
        contract.setReservation(reservation);
        contract.setLanguage(language);
        contract.setObjectPath("100/" + language.name().toLowerCase() + "/contract-" + (reservation != null ? reservation.getCode() : "TEST") + ".pdf");
        contract.setAccessToken("token123");
        return contract;
    }

    public static EmailLog createEmailLog(Long reservationId, String recipient, String status) {
        return new EmailLog(
                reservationId,
                recipient,
                "Confirmation de réservation",
                "mail/reservation-confirmation",
                status,
                null,
                1
        );
    }
}
