package ma.locavite.payment.cmi.repository;

import ma.locavite.payment.cmi.domain.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {

    Optional<IdempotencyKey> findByKey(String key);

    @Modifying
    @Query("DELETE FROM IdempotencyKey k WHERE k.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") Instant cutoff);
}
