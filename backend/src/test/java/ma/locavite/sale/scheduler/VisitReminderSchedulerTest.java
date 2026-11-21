package ma.locavite.sale.scheduler;

import ma.locavite.sale.service.VisitService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class VisitReminderSchedulerTest {

    @Test
    void sendsDailyReminderForTomorrow() {
        VisitService visitService = mock(VisitService.class);
        VisitReminderScheduler scheduler = new VisitReminderScheduler(visitService);
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        when(visitService.sendReminders(tomorrow)).thenReturn(2);

        assertEquals(2, scheduler.sendRemindersForDate(tomorrow));
        verify(visitService).sendReminders(tomorrow);
    }
}
