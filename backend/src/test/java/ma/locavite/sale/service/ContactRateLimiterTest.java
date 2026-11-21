package ma.locavite.sale.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContactRateLimiterTest {

    @Test
    void allowsFiveRequestsPerMinuteAndRejectsTheSixthForTheSameIp() {
        ContactRateLimiter limiter = new ContactRateLimiter(null);

        for (int request = 0; request < 5; request++) {
            assertTrue(limiter.tryConsume("192.0.2.1"));
        }
        assertFalse(limiter.tryConsume("192.0.2.1"));
        assertTrue(limiter.tryConsume("192.0.2.2"));
    }
}
