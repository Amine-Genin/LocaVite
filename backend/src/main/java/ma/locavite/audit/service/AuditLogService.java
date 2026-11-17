package ma.locavite.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.locavite.audit.domain.AuditLog;
import ma.locavite.audit.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuditLogService {

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long userId, String action, String entity, String entityId,
                       String ip, String userAgent, Object details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setEntity(entity);
        auditLog.setEntityId(entityId);
        auditLog.setIp(ip);
        auditLog.setUserAgent(userAgent);
        auditLog.setTimestamp(Instant.now());
        try {
            auditLog.setDetails(objectMapper.writeValueAsString(details));
        } catch (Exception exception) {
            throw new IllegalStateException("Impossible de sérialiser le détail de l'audit", exception);
        }
        repository.save(auditLog);
    }
}
