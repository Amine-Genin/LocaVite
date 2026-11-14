package ma.locavite.payment.cmi.filter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import ma.locavite.payment.cmi.config.CmiProperties;
import ma.locavite.payment.cmi.service.CmiHashService;
import ma.locavite.payment.cmi.service.CmiIpWhitelistService;
import ma.locavite.payment.cmi.service.CmiSecurityAlertService;
import ma.locavite.payment.cmi.service.CmiWebhookRateLimiter;
import ma.locavite.payment.cmi.util.CmiDataSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Filtre de sécurité dédié au webhook CMI (/api/payments/cmi/webhook).
 * Assure :
 * 1. Journalisation complète (payload + headers + IP réelle via X-Forwarded-For)
 * 2. Vérification HTTPS obligatoire (TLS 1.2+ / X-Forwarded-Proto) lorsque activée
 * 3. Vérification de la liste blanche IP CMI (403 Forbidden si non autorisée)
 * 4. Rate limiting de 10 requêtes / minute par IP (429 Too Many Requests)
 * 5. Vérification de la signature HMAC-SHA512 (401 Unauthorized si invalide)
 * 6. Déclenchement d'alertes Prometheus / Alertmanager en cas d'échec
 */
@Component
public class CmiWebhookFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CmiWebhookFilter.class);

    public static final String WEBHOOK_PATH = "/api/payments/cmi/webhook";
    public static final String ATTR_WEBHOOK_VERIFIED = "cmi.webhook.filter.verified";
    public static final String ATTR_WEBHOOK_RAW_PAYLOAD = "cmi.webhook.raw.payload";
    public static final String ATTR_WEBHOOK_CLIENT_IP = "cmi.webhook.client.ip";

    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {};

    private final CmiProperties cmiProperties;
    private final CmiIpWhitelistService ipWhitelistService;
    private final CmiWebhookRateLimiter rateLimiter;
    private final CmiHashService hashService;
    private final CmiSecurityAlertService securityAlertService;
    private final ObjectMapper objectMapper;

    public CmiWebhookFilter(
            CmiProperties cmiProperties,
            CmiIpWhitelistService ipWhitelistService,
            CmiWebhookRateLimiter rateLimiter,
            CmiHashService hashService,
            CmiSecurityAlertService securityAlertService,
            ObjectMapper objectMapper) {
        this.cmiProperties = cmiProperties;
        this.ipWhitelistService = ipWhitelistService;
        this.rateLimiter = rateLimiter;
        this.hashService = hashService;
        this.securityAlertService = securityAlertService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.endsWith(WEBHOOK_PATH);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        CachedBodyWebhookRequest wrappedRequest = new CachedBodyWebhookRequest(request);
        String clientIp = ipWhitelistService.getClientIp(wrappedRequest);
        Map<String, String> headers = extractHeaders(wrappedRequest);
        Map<String, String> params = extractAllParams(wrappedRequest);
        String sanitizedPayload = buildSanitizedPayloadString(params, wrappedRequest.getCachedBodyAsString());

        // 1. Log complet (payload + headers + IP)
        log.info("AUDIT_WEBHOOK_CMI ip={} method={} uri={} proto={} headers={} payload={}",
                clientIp,
                wrappedRequest.getMethod(),
                wrappedRequest.getRequestURI(),
                getProtocol(wrappedRequest),
                headers,
                sanitizedPayload);

        // 2. Vérification HTTPS obligatoire (si activée en configuration)
        if (cmiProperties.isWebhookRequireHttps() && !isHttpsRequest(wrappedRequest)) {
            securityAlertService.triggerSecurityAlert(
                    "https_required",
                    clientIp,
                    "Tentative d'appel au webhook CMI en HTTP non chiffré"
            );
            writeJsonError(response, HttpStatus.FORBIDDEN, "HTTPS (TLS 1.2+) est obligatoire pour le webhook CMI");
            return;
        }

        // 3. Vérification Whitelist IP CMI → 403 Forbidden
        if (!ipWhitelistService.isIpAllowed(clientIp)) {
            securityAlertService.triggerSecurityAlert(
                    "ip_not_whitelisted",
                    clientIp,
                    "IP non autorisée pour le webhook CMI: " + clientIp
            );
            writeJsonError(response, HttpStatus.FORBIDDEN, "IP non autorisée pour le webhook CMI : " + clientIp);
            return;
        }

        // 4. Rate Limiting (10 req/min par IP) → 429 Too Many Requests
        if (!rateLimiter.tryConsume(clientIp)) {
            securityAlertService.triggerSecurityAlert(
                    "rate_limit_exceeded",
                    clientIp,
                    "Dépassement de la limite de " + cmiProperties.getWebhookRateLimitPerMinute() + " req/min pour l'IP " + clientIp
            );
            response.setHeader("Retry-After", "60");
            writeJsonError(response, HttpStatus.TOO_MANY_REQUESTS,
                    "Trop de requêtes sur le webhook CMI depuis l'IP " + clientIp + " (limite : " + cmiProperties.getWebhookRateLimitPerMinute() + " req/min)");
            return;
        }

        // 5. Vérification de la signature HMAC-SHA512 → 401 Unauthorized
        if (!hashService.verifyHash(params, cmiProperties.getStoreKey())) {
            securityAlertService.triggerSecurityAlert(
                    "invalid_signature",
                    clientIp,
                    "Signature HMAC-SHA512 absente ou invalide pour oid=" + params.getOrDefault("oid", "unknown")
            );
            writeJsonError(response, HttpStatus.UNAUTHORIZED, "Signature CMI invalide ou absente");
            return;
        }

        securityAlertService.recordWebhookRequest("accepted");
        wrappedRequest.setAttribute(ATTR_WEBHOOK_VERIFIED, Boolean.TRUE);
        wrappedRequest.setAttribute(ATTR_WEBHOOK_RAW_PAYLOAD, sanitizedPayload);
        wrappedRequest.setAttribute(ATTR_WEBHOOK_CLIENT_IP, clientIp);

        filterChain.doFilter(wrappedRequest, response);
    }

    private boolean isHttpsRequest(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return "https".equalsIgnoreCase(forwardedProto);
    }

    private String getProtocol(HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto != null && !forwardedProto.isBlank()) {
            return forwardedProto;
        }
        return request.getScheme();
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        if (names != null) {
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                String lower = name.toLowerCase(Locale.ROOT);
                if (lower.contains("authorization") || lower.contains("cookie") || lower.contains("secret")) {
                    headers.put(name, "******");
                } else {
                    headers.put(name, request.getHeader(name));
                }
            }
        }
        return headers;
    }

    private Map<String, String> extractAllParams(CachedBodyWebhookRequest request) {
        Map<String, String> merged = new LinkedHashMap<>();

        Map<String, String[]> paramMap = request.getParameterMap();
        if (paramMap != null) {
            for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
                if (entry.getValue() != null && entry.getValue().length > 0) {
                    merged.put(entry.getKey(), entry.getValue()[0]);
                }
            }
        }

        String rawBody = request.getCachedBodyAsString();
        if (rawBody != null && !rawBody.isBlank()) {
            String contentType = request.getContentType() != null ? request.getContentType().toLowerCase(Locale.ROOT) : "";
            if (contentType.contains(MediaType.APPLICATION_JSON_VALUE) || rawBody.trim().startsWith("{")) {
                try {
                    Map<String, String> jsonMap = objectMapper.readValue(rawBody, STRING_MAP);
                    if (jsonMap != null) {
                        merged.putAll(jsonMap);
                    }
                } catch (Exception e) {
                    log.debug("Impossible de parser le corps JSON dans CmiWebhookFilter: {}", e.getMessage());
                }
            } else if (contentType.contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE) || rawBody.contains("=")) {
                parseFormUrlEncoded(rawBody, merged);
            }
        }

        return merged;
    }

    private void parseFormUrlEncoded(String body, Map<String, String> target) {
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            if (pair.isBlank()) continue;
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                target.putIfAbsent(key, value);
            }
        }
    }

    private String buildSanitizedPayloadString(Map<String, String> params, String rawBody) {
        try {
            Map<String, String> sanitized = CmiDataSanitizer.maskSensitiveParams(params);
            if (!sanitized.isEmpty()) {
                return objectMapper.writeValueAsString(sanitized);
            }
        } catch (Exception e) {
            log.debug("Erreur sérialisation payload CMI: {}", e.getMessage());
        }
        return rawBody != null ? rawBody : "{}";
    }

    private void writeJsonError(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("detail", detail);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    /**
     * Wrapper permettant de relire le corps HTTP à la fois dans le filtre et dans le contrôleur Spring.
     */
    public static class CachedBodyWebhookRequest extends HttpServletRequestWrapper {

        private final byte[] cachedBody;
        private final Map<String, String[]> cachedParameterMap;

        public CachedBodyWebhookRequest(HttpServletRequest request) throws IOException {
            super(request);
            // Force l'extraction des paramètres de formulaire avant lecture du flux si applicable
            Map<String, String[]> origParams = request.getParameterMap();
            this.cachedParameterMap = new LinkedHashMap<>(origParams != null ? origParams : Collections.emptyMap());
            this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
        }

        public String getCachedBodyAsString() {
            return new String(this.cachedBody, StandardCharsets.UTF_8);
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            return Collections.unmodifiableMap(this.cachedParameterMap);
        }

        @Override
        public String getParameter(String name) {
            String[] values = this.cachedParameterMap.get(name);
            return (values != null && values.length > 0) ? values[0] : super.getParameter(name);
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return Collections.enumeration(this.cachedParameterMap.keySet());
        }

        @Override
        public String[] getParameterValues(String name) {
            return this.cachedParameterMap.get(name);
        }

        @Override
        public ServletInputStream getInputStream() {
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.cachedBody);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // Non bloquant en mémoire
                }

                @Override
                public int read() {
                    return byteArrayInputStream.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
