package ma.locavite.audit.aspect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.servlet.http.HttpServletRequest;
import ma.locavite.audit.annotation.Auditable;
import ma.locavite.audit.service.AuditLogService;
import ma.locavite.vehicle.service.CurrentUserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Iterator;
import java.util.Map;
import java.util.Arrays;

@Aspect
@Component
public class AuditAspect {

    private static final String MASKED = "[REDACTED]";
    private static final String[] SENSITIVE_NAMES = {
            "pan", "cardnumber", "card_number", "cvv", "cvc", "cavv",
            "expiry", "expdate", "password", "secret", "token", "authorization"
    };

    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    public AuditAspect(AuditLogService auditLogService, CurrentUserService currentUserService,
                       ObjectMapper objectMapper) {
        this.auditLogService = auditLogService;
        this.currentUserService = currentUserService;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        Object result = null;
        Throwable failure = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable throwable) {
            failure = throwable;
            throw throwable;
        } finally {
            record(joinPoint, auditable, result, failure);
        }
    }

    private void record(ProceedingJoinPoint joinPoint, Auditable auditable,
                        Object result, Throwable failure) {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
            ObjectNode details = objectMapper.createObjectNode();
            details.put("method", joinPoint.getSignature().toShortString());
            details.put("outcome", failure == null ? "SUCCESS" : "FAILURE");
            if (failure != null) {
                details.put("error", failure.getClass().getSimpleName());
            }
            details.set("arguments", sanitizeArguments(joinPoint.getArgs()));

            String entityId = resolveEntityId(joinPoint.getArgs(), result, auditable.entityIdParameter());
            auditLogService.record(
                    currentUserService.getCurrentUserId().orElse(null),
                    auditable.action(),
                    auditable.entity(),
                    entityId,
                    request != null ? request.getRemoteAddr() : null,
                    request != null ? truncate(request.getHeader("User-Agent"), 512) : null,
                    details
            );
        } catch (RuntimeException auditFailure) {
            throw auditFailure;
        }
    }

    private ArrayNode sanitizeArguments(Object[] arguments) {
        ArrayNode sanitized = objectMapper.createArrayNode();
        Arrays.stream(arguments).forEach(argument -> {
            if (argument == null) {
                sanitized.addNull();
            } else if (argument instanceof HttpServletRequest) {
                sanitized.add("[HTTP_REQUEST]");
            } else if (argument instanceof byte[]) {
                sanitized.add("[BINARY_DATA]");
            } else {
                sanitized.add(sanitize(argument));
            }
        });
        return sanitized;
    }

    private JsonNode sanitize(Object value) {
        JsonNode node = objectMapper.valueToTree(value);
        if (node != null) {
            sanitizeNode(node);
        }
        return node;
    }

    private void sanitizeNode(JsonNode node) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (isSensitive(field.getKey())) {
                    ((ObjectNode) node).put(field.getKey(), MASKED);
                } else if (field.getValue().isTextual() && looksLikeCardData(field.getValue().textValue())) {
                    ((ObjectNode) node).put(field.getKey(), MASKED);
                } else {
                    sanitizeNode(field.getValue());
                }
            }
        } else if (node.isArray()) {
            ArrayNode array = (ArrayNode) node;
            for (int i = 0; i < array.size(); i++) {
                if (array.get(i).isTextual() && looksLikeCardData(array.get(i).textValue())) {
                    array.set(i, MASKED);
                } else {
                    sanitizeNode(array.get(i));
                }
            }
        }
    }

    private boolean isSensitive(String name) {
        String normalized = name.toLowerCase().replace("-", "");
        for (String sensitiveName : SENSITIVE_NAMES) {
            if (normalized.contains(sensitiveName)) {
                return true;
            }
        }
        return false;
    }

    private boolean looksLikeCardData(String value) {
        String digits = value.replaceAll("[ -]", "");
        return digits.matches("\\d{13,19}");
    }

    private String resolveEntityId(Object[] args, Object result, int parameterIndex) {
        if (parameterIndex >= 0 && parameterIndex < args.length && args[parameterIndex] != null) {
            return String.valueOf(args[parameterIndex]);
        }
        if (result != null) {
            try {
                Object id;
                try {
                    id = result.getClass().getMethod("id").invoke(result);
                } catch (NoSuchMethodException ignored) {
                    id = result.getClass().getMethod("getId").invoke(result);
                }
                return id != null ? String.valueOf(id) : null;
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }
        return null;
    }

    private String truncate(String value, int maxLength) {
        return value == null ? null : value.substring(0, Math.min(value.length(), maxLength));
    }
}
