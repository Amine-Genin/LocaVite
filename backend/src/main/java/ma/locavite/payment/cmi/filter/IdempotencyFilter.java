package ma.locavite.payment.cmi.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ma.locavite.payment.cmi.domain.IdempotencyKey;
import ma.locavite.payment.cmi.repository.IdempotencyKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Filtre garantissant l'idempotence des requêtes d'initiation de paiement CMI.
 * 
 * - Intercepte POST /api/payments/cmi/initiate
 * - Exige la présence du header HTTP Idempotency-Key (400 Bad Request si absent ou vide)
 * - Retourne la réponse mise en cache (200 OK) si la même clé est réémise sous 24h
 * - Sauvegarde la réponse (200 OK) en base dans la table idempotency_keys
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyFilter.class);

    public static final String INITIATE_PATH = "/api/payments/cmi/initiate";
    public static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    @Autowired
    public IdempotencyFilter(
            IdempotencyKeyRepository idempotencyKeyRepository,
            ObjectMapper objectMapper,
            @Value("${cmi.idempotency.ttl-hours:24}") long ttlHours) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofHours(ttlHours);
    }

    public IdempotencyFilter(
            IdempotencyKeyRepository idempotencyKeyRepository,
            ObjectMapper objectMapper) {
        this(idempotencyKeyRepository, objectMapper, 24);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        return uri == null || !uri.endsWith(INITIATE_PATH);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String idempotencyKey = request.getHeader(HEADER_IDEMPOTENCY_KEY);
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            log.warn("Appel à {} sans header obligatoire '{}'", INITIATE_PATH, HEADER_IDEMPOTENCY_KEY);
            writeJsonError(response, HttpStatus.BAD_REQUEST, "Le header '" + HEADER_IDEMPOTENCY_KEY + "' est obligatoire pour initier un paiement CMI");
            return;
        }
        idempotencyKey = idempotencyKey.trim();

        // 1. Vérifier si la clé existe déjà en base
        Optional<IdempotencyKey> existingKey = idempotencyKeyRepository.findByKey(idempotencyKey);
        if (existingKey.isPresent()) {
            IdempotencyKey cached = existingKey.get();
            if (!cached.isExpired(ttl)) {
                log.info("Idempotence CMI détectée pour la clé '{}' : retour de la réponse mise en cache (status={})",
                        idempotencyKey, cached.getStatusCode());
                response.setStatus(cached.getStatusCode());
                response.setContentType(cached.getContentType() != null ? cached.getContentType() : MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.setHeader("Idempotent-Replay", "true");
                response.getWriter().write(cached.getResponse());
                response.getWriter().flush();
                return;
            } else {
                log.info("Clé d'idempotence '{}' expirée (> {}), suppression et réexécution", idempotencyKey, ttl);
                try {
                    idempotencyKeyRepository.delete(cached);
                } catch (Exception e) {
                    log.warn("Erreur suppression clé expirée {} : {}", idempotencyKey, e.getMessage());
                }
            }
        }

        // 2. Première requête avec cette clé : intercepter la réponse pour la sauvegarder
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(request, responseWrapper);
        } finally {
            int status = responseWrapper.getStatus();
            if (status >= 200 && status < 300) {
                byte[] content = responseWrapper.getContentAsByteArray();
                if (content.length > 0) {
                    String responseBody = new String(content, StandardCharsets.UTF_8);
                    String contentType = responseWrapper.getContentType();
                    try {
                        IdempotencyKey record = new IdempotencyKey(
                                idempotencyKey,
                                responseBody,
                                status,
                                contentType != null ? contentType : MediaType.APPLICATION_JSON_VALUE
                        );
                        idempotencyKeyRepository.save(record);
                        log.info("Clé d'idempotence '{}' enregistrée avec succès (status={})", idempotencyKey, status);
                    } catch (Exception e) {
                        log.warn("Impossible d'enregistrer la clé d'idempotence '{}' (possible doublon concurrent) : {}",
                                idempotencyKey, e.getMessage());
                    }
                }
            }
            responseWrapper.copyBodyToResponse();
        }
    }

    private void writeJsonError(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "about:blank");
        body.put("title", status.getReasonPhrase());
        body.put("status", status.value());
        body.put("detail", detail);
        body.put("message", detail);
        body.put("error", status.getReasonPhrase());
        response.getWriter().write(objectMapper.writeValueAsString(body));
        response.getWriter().flush();
    }
}
