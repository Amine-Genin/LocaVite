package ma.locavite.payment.cmi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.locavite.payment.cmi.dto.CmiInitiateRequest;
import ma.locavite.payment.cmi.dto.CmiInitiateResponse;
import ma.locavite.payment.cmi.dto.CmiWebhookResponse;
import ma.locavite.payment.cmi.exception.CmiIpNotAllowedException;
import ma.locavite.payment.cmi.exception.CmiRateLimitExceededException;
import ma.locavite.payment.cmi.exception.CmiSignatureException;
import ma.locavite.payment.cmi.exception.CmiUnavailableException;
import ma.locavite.payment.cmi.exception.CmiWebhookSignatureException;
import ma.locavite.payment.cmi.service.CmiPaymentService;
import ma.locavite.payment.domain.PaymentStatus;
import ma.locavite.payment.exception.PaymentExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CmiPaymentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CmiPaymentService cmiPaymentService;

    @InjectMocks
    private CmiPaymentController cmiPaymentController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cmiPaymentController)
                .setControllerAdvice(new PaymentExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Endpoint POST /api/payments/cmi/initiate → retourne 200 avec URL CMI générée et formData")
    void initiate_returns200WithGatewayUrlAndSignature() throws Exception {
        CmiInitiateRequest request = new CmiInitiateRequest(
                10L,
                new BigDecimal("500.00"),
                "https://locavite.ma/reservations/10/confirmation"
        );

        Map<String, String> formData = Map.of(
                "clientid", "merchant_123",
                "amount", "500.00",
                "oid", "PAY-CMI001",
                "currency", "504",
                "hash", "HMAC_HASH_12345=="
        );

        CmiInitiateResponse response = new CmiInitiateResponse(
                42L,
                "PAY-CMI001",
                new BigDecimal("500.00"),
                "MAD",
                "https://testpayment.cmi.co.ma/fim/est3Dgate",
                "https://testpayment.cmi.co.ma/fim/est3Dgate?oid=PAY-CMI001",
                formData,
                Instant.now().plusSeconds(900)
        );

        when(cmiPaymentService.initiate(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/payments/cmi/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(42))
                .andExpect(jsonPath("$.paymentCode").value("PAY-CMI001"))
                .andExpect(jsonPath("$.gatewayUrl").value("https://testpayment.cmi.co.ma/fim/est3Dgate"))
                .andExpect(jsonPath("$.redirectUrl", containsString("https://testpayment.cmi.co.ma/fim/est3Dgate")))
                .andExpect(jsonPath("$.formData.hash").value("HMAC_HASH_12345=="))
                .andExpect(jsonPath("$.formData.clientid").value("merchant_123"));
    }

    @Test
    @DisplayName("Endpoint POST /api/payments/cmi/initiate → 503 si passerelle CMI indisponible (mode fallback)")
    void initiate_cmiUnavailable_returns503ServiceUnavailable() throws Exception {
        CmiInitiateRequest request = new CmiInitiateRequest(
                10L,
                new BigDecimal("500.00"),
                "https://locavite.ma/confirmation"
        );

        when(cmiPaymentService.initiate(any(), any()))
                .thenThrow(new CmiUnavailableException("Passerelle CMI temporairement indisponible"));

        mockMvc.perform(post("/api/payments/cmi/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail", containsString("Passerelle CMI temporairement indisponible")));
    }

    @Test
    @DisplayName("Endpoint POST /api/payments/cmi/callback avec hash valide → 302 Redirection vers confirmation")
    void callback_validHash_redirectsToConfirmationPage() throws Exception {
        String expectedRedirect = "https://locavite.ma/confirmation?paymentId=42&code=PAY-CMI001&status=SUCCESS";
        when(cmiPaymentService.handleCallback(any(), any())).thenReturn(expectedRedirect);

        mockMvc.perform(post("/api/payments/cmi/callback")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("oid", "PAY-CMI001")
                        .param("Response", "Approved")
                        .param("ProcReturnCode", "00")
                        .param("TransId", "TXN9988")
                        .param("hash", "VALID_HASH=="))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", expectedRedirect));
    }

    @Test
    @DisplayName("Endpoint GET /api/payments/cmi/callback → 302 Redirection supportée")
    void callbackGet_redirectsToConfirmationPage() throws Exception {
        String expectedRedirect = "https://locavite.ma/confirmation?paymentId=42&status=SUCCESS";
        when(cmiPaymentService.handleCallback(any(), any())).thenReturn(expectedRedirect);

        mockMvc.perform(get("/api/payments/cmi/callback")
                        .param("oid", "PAY-CMI001")
                        .param("ProcReturnCode", "00")
                        .param("hash", "VALID_HASH=="))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", expectedRedirect));
    }

    @Test
    @DisplayName("Endpoint POST /api/payments/cmi/callback avec hash invalide → 400 Bad Request")
    void callback_invalidHash_returns400BadRequest() throws Exception {
        when(cmiPaymentService.handleCallback(any(), any()))
                .thenThrow(new CmiSignatureException("Signature CMI invalide ou altérée"));

        mockMvc.perform(post("/api/payments/cmi/callback")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("oid", "PAY-CMI001")
                        .param("hash", "BAD_HASH"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Signature CMI invalide ou altérée")));
    }

    @Test
    @DisplayName("Endpoint POST /api/payments/cmi/webhook → 200 OK avec ACTION=POSTAUTH")
    void webhook_validRequest_returnsActionPostAuth() throws Exception {
        CmiWebhookResponse webhookResponse = new CmiWebhookResponse("ACTION=POSTAUTH", false, 42L, PaymentStatus.SUCCESS);
        when(cmiPaymentService.handleWebhook(any(), any())).thenReturn(webhookResponse);

        mockMvc.perform(post("/api/payments/cmi/webhook")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("oid", "PAY-CMI001")
                        .param("Response", "Approved")
                        .param("ProcReturnCode", "00")
                        .param("hash", "VALID_HASH"))
                .andExpect(status().isOk())
                .andExpect(content().string("ACTION=POSTAUTH"));
    }

    @Test
    @DisplayName("Endpoint POST /api/payments/cmi/webhook avec IP non autorisée → 403 Forbidden")
    void webhook_unauthorizedIp_returns403Forbidden() throws Exception {
        when(cmiPaymentService.handleWebhook(any(), any()))
                .thenThrow(new CmiIpNotAllowedException("IP non autorisée pour le webhook CMI : 198.51.100.25"));

        mockMvc.perform(post("/api/payments/cmi/webhook")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("oid", "PAY-CMI001")
                        .param("hash", "VALID_HASH"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail", containsString("IP non autorisée")));
    }

    @Test
    @DisplayName("Endpoint POST /api/payments/cmi/webhook avec signature invalide → 401 Unauthorized")
    void webhook_invalidSignature_returns401Unauthorized() throws Exception {
        when(cmiPaymentService.handleWebhook(any(), any()))
                .thenThrow(new CmiWebhookSignatureException("Signature CMI invalide"));

        mockMvc.perform(post("/api/payments/cmi/webhook")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("oid", "PAY-CMI001")
                        .param("hash", "BAD_SIG"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail", containsString("Signature CMI invalide")));
    }

    @Test
    @DisplayName("Endpoint POST /api/payments/cmi/webhook avec rate limit dépassé → 429 Too Many Requests")
    void webhook_rateLimitExceeded_returns429TooManyRequests() throws Exception {
        when(cmiPaymentService.handleWebhook(any(), any()))
                .thenThrow(new CmiRateLimitExceededException("Trop de requêtes sur le webhook CMI (limite : 10 req/min)"));

        mockMvc.perform(post("/api/payments/cmi/webhook")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("oid", "PAY-CMI001")
                        .param("hash", "VALID_HASH"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.detail", containsString("Trop de requêtes")));
    }
}
