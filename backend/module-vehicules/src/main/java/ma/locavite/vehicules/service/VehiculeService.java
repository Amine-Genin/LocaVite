package ma.locavite.vehicules.service;

import lombok.extern.slf4j.Slf4j;
import ma.locavite.vehicules.domain.Vehicule;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class VehiculeService {

    private final ConcurrentHashMap<Long, Vehicule> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public List<Vehicule> getVehiculesDisponibles() {
        return storage.values().stream()
                .filter(Vehicule::getDisponible)
                .toList();
    }

    public Optional<Vehicule> getVehiculeById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public Vehicule enregistrerVehicule(Vehicule vehicule) {
        if (vehicule.getId() == null) {
            vehicule.setId(idGenerator.getAndIncrement());
        }
        storage.put(vehicule.getId(), vehicule);
        return vehicule;
    }
}
