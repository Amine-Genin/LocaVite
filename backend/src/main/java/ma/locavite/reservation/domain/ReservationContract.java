package ma.locavite.reservation.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "reservation_contracts",
        uniqueConstraints = @UniqueConstraint(name = "uk_contract_reservation_language",
                columnNames = {"reservation_id", "language"}))
public class ReservationContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, length = 2)
    private ContractLanguage language;

    @Column(name = "object_path", nullable = false, unique = true, length = 512)
    private String objectPath;

    @Column(name = "access_token", nullable = false, unique = true, length = 64)
    private String accessToken;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Reservation getReservation() { return reservation; }
    public void setReservation(Reservation reservation) { this.reservation = reservation; }
    public ContractLanguage getLanguage() { return language; }
    public void setLanguage(ContractLanguage language) { this.language = language; }
    public String getObjectPath() { return objectPath; }
    public void setObjectPath(String objectPath) { this.objectPath = objectPath; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
}
