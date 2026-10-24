package ma.locavite.vente.service;

import lombok.extern.slf4j.Slf4j;
import ma.locavite.vente.domain.AnnonceVente;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class VenteService {

    private final ConcurrentHashMap<Long, AnnonceVente> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public List<AnnonceVente> getAnnoncesActives() {
        return storage.values().stream()
                .filter(AnnonceVente::getActive)
                .toList();
    }

    public Optional<AnnonceVente> getAnnonceById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public AnnonceVente publierAnnonce(AnnonceVente annonce) {
        if (annonce.getId() == null) {
            annonce.setId(idGenerator.getAndIncrement());
        }
        storage.put(annonce.getId(), annonce);
        return annonce;
    }
}
