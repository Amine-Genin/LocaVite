package ma.locavite.paiement.service;

import lombok.extern.slf4j.Slf4j;
import ma.locavite.paiement.domain.Paiement;
import ma.locavite.paiement.domain.PaiementStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class PaiementService {

    private final ConcurrentHashMap<Long, Paiement> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public Optional<Paiement> getPaiementById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    public Paiement initierPaiement(Paiement paiement) {
        if (paiement.getId() == null) {
            paiement.setId(idGenerator.getAndIncrement());
        }
        if (paiement.getReferenceTransaction() == null || paiement.getReferenceTransaction().isBlank()) {
            paiement.setReferenceTransaction("TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (paiement.getStatut() == null) {
            paiement.setStatut(PaiementStatus.PENDING);
        }
        storage.put(paiement.getId(), paiement);
        return paiement;
    }

    public Paiement validerPaiement(Long id) {
        Paiement p = storage.get(id);
        if (p == null) {
            throw new IllegalArgumentException("Paiement non trouvé : " + id);
        }
        p.setStatut(PaiementStatus.SUCCEEDED);
        return p;
    }
}
