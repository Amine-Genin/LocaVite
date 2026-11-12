package ma.locavite.notification.repository;

import ma.locavite.notification.domain.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    List<NotificationLog> findByReservationId(Long reservationId);

    List<NotificationLog> findByUserId(Long userId);

    List<NotificationLog> findByRecipient(String recipient);

    List<NotificationLog> findByStatus(String status);

    Page<NotificationLog> findByReservationId(Long reservationId, Pageable pageable);

    boolean existsByReservationIdAndNotificationTypeAndStatus(Long reservationId, String notificationType, String status);
}
