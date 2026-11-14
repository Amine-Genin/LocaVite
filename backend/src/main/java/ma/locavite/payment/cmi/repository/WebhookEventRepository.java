package ma.locavite.payment.cmi.repository;

import ma.locavite.payment.cmi.domain.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    boolean existsByEventId(String eventId);

    Optional<WebhookEvent> findByEventId(String eventId);
}
