package ma.locavite.vehicle.domain;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
@Entity
@Table(name = "vehicle_incidents")
public class VehicleIncident {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "vehicle_id", nullable = false) private Long vehicleId;
    @Column(nullable = false) private LocalDate date;
    @Column(nullable = false, length = 1000) private String description;
    @Column(nullable = false, length = 20) private String gravite;
    @Column(name = "created_by", nullable = false) private Long createdBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    public Long getId() { return id; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long value) { vehicleId = value; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate value) { date = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public String getGravite() { return gravite; }
    public void setGravite(String value) { gravite = value; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long value) { createdBy = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { createdAt = value; }
}
