package ma.locavite.vehicle.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "vehicle_status_history")
public class VehicleStatusHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "vehicle_id", nullable = false) private Long vehicleId;
    @Enumerated(EnumType.STRING) @Column(name = "ancien_statut", length = 20) private VehicleStatus ancienStatut;
    @Enumerated(EnumType.STRING) @Column(name = "nouveau_statut", nullable = false, length = 20) private VehicleStatus nouveauStatut;
    @Column(length = 500) private String raison;
    @Column(nullable = false) private Instant date;
    @Column(name = "user_id", nullable = false) private Long userId;
    public Long getId() { return id; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long value) { vehicleId = value; }
    public VehicleStatus getAncienStatut() { return ancienStatut; }
    public void setAncienStatut(VehicleStatus value) { ancienStatut = value; }
    public VehicleStatus getNouveauStatut() { return nouveauStatut; }
    public void setNouveauStatut(VehicleStatus value) { nouveauStatut = value; }
    public String getRaison() { return raison; }
    public void setRaison(String value) { raison = value; }
    public Instant getDate() { return date; }
    public void setDate(Instant value) { date = value; }
    public Long getUserId() { return userId; }
    public void setUserId(Long value) { userId = value; }
}
