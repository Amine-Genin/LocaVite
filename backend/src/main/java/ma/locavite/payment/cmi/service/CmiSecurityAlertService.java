package ma.locavite.payment.cmi.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CmiSecurityAlertService {

    private static final Logger log = LoggerFactory.getLogger(CmiSecurityAlertService.class);

    public static final String METRIC_SECURITY_FAILURES = "cmi_webhook_security_failures_total";
    public static final String METRIC_WEBHOOK_REQUESTS = "cmi_webhook_requests_total";

    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> failureCounters = new ConcurrentHashMap<>();

    public CmiSecurityAlertService() {
        this(new SimpleMeterRegistry());
    }

    @Autowired
    public CmiSecurityAlertService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry != null ? meterRegistry : new SimpleMeterRegistry();
    }

    public void recordWebhookRequest(String status) {
        Counter.builder(METRIC_WEBHOOK_REQUESTS)
                .description("Nombre total de requêtes reçues sur le webhook CMI")
                .tag("status", status != null ? status : "unknown")
                .register(meterRegistry)
                .increment();
    }

    public void triggerSecurityAlert(String reason, String clientIp, String details) {
        String normalizedReason = reason != null ? reason : "unknown";

        Counter counter = failureCounters.computeIfAbsent(normalizedReason, r ->
                Counter.builder(METRIC_SECURITY_FAILURES)
                        .description("Nombre d'échecs de vérification de sécurité sur le webhook CMI (Prometheus / Alertmanager)")
                        .tag("reason", r)
                        .register(meterRegistry)
        );
        counter.increment();

        recordWebhookRequest("rejected_" + normalizedReason);

        log.error("SECURITY_ALERT [CMI_WEBHOOK] timestamp={} reason={} ip={} details=\"{}\"",
                Instant.now(), normalizedReason, clientIp, details);
    }

    public double getFailureCount(String reason) {
        Counter counter = meterRegistry.find(METRIC_SECURITY_FAILURES).tag("reason", reason).counter();
        return counter != null ? counter.count() : 0.0;
    }

    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }
}
