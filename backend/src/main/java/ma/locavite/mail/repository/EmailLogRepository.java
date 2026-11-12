package ma.locavite.mail.repository;

import ma.locavite.mail.domain.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    List<EmailLog> findByReservationId(Long reservationId);
    List<EmailLog> findByRecipient(String recipient);
    List<EmailLog> findByReservationIdOrderByCreatedAtDesc(Long reservationId);
}
