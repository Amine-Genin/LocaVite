package ma.locavite.payment;

import ma.locavite.audit.domain.AuditLog;
import ma.locavite.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.cache.type=none")
class AuditLogIT {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void auditLogSchemaStoresRequiredFieldsAndNoCardData() {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction("PAYMENT_READ");
        auditLog.setEntity("Payment");
        auditLog.setEntityId("42");
        auditLog.setIp("127.0.0.1");
        auditLog.setUserAgent("JUnit");
        auditLog.setDetails("{\"arguments\":[{\"metadata\":\"[REDACTED]\"}]}");
        auditLog.setTimestamp(java.time.Instant.now());

        AuditLog saved = auditLogRepository.saveAndFlush(auditLog);
        List<AuditLog> stored = auditLogRepository.findAll();

        assertThat(saved.getId()).isNotNull();
        assertThat(stored).anySatisfy(entry -> {
            assertThat(entry.getAction()).isEqualTo("PAYMENT_READ");
            assertThat(entry.getEntity()).isEqualTo("Payment");
            assertThat(entry.getDetails()).doesNotContain("4111111111111111");
        });
    }
}
