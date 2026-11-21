package ma.locavite.sale.controller;

import jakarta.servlet.http.HttpServletRequest;
import ma.locavite.sale.dto.ContactRequestBody;
import ma.locavite.sale.dto.ContactRequestResponse;
import ma.locavite.sale.domain.ContactStatus;
import ma.locavite.sale.service.ContactRateLimiter;
import ma.locavite.sale.service.ContactRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContactRequestControllerTest {

    @Test
    void returnsCreatedWithTheNewRequestId() {
        ContactRequestService service = mock(ContactRequestService.class);
        ContactRateLimiter rateLimiter = mock(ContactRateLimiter.class);
        ContactRequestController controller = new ContactRequestController(service, rateLimiter);
        ContactRequestBody body = new ContactRequestBody("Karim", "karim@example.com", "+212600000000", "Question");
        ContactRequestResponse response = new ContactRequestResponse(
                41L, 7L, "Karim", "karim@example.com", "+212600000000", "Question",
                ContactStatus.NEW, Instant.now(), null, null);
        HttpServletRequest request = new MockHttpServletRequest();

        when(rateLimiter.tryConsume("127.0.0.1")).thenReturn(true);
        when(service.create(7L, body)).thenReturn(response);

        var result = controller.create(7L, body, request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(41L, result.getBody().id());
    }

    @Test
    void rejectsTheRequestWhenTheIpRateLimitIsExceeded() {
        ContactRateLimiter rateLimiter = mock(ContactRateLimiter.class);
        ContactRequestService service = mock(ContactRequestService.class);
        ContactRequestController controller = new ContactRequestController(service, rateLimiter);
        when(rateLimiter.tryConsume("127.0.0.1")).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.create(
                        7L,
                        new ContactRequestBody("Karim", "karim@example.com", "+212600000000", "Question"),
                        new MockHttpServletRequest()));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
        verifyNoInteractions(service);
    }
}
