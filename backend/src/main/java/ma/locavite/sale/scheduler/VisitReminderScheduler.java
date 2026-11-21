package ma.locavite.sale.scheduler;

import ma.locavite.sale.service.VisitService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class VisitReminderScheduler {

    private final VisitService visitService;

    public VisitReminderScheduler(VisitService visitService) {
        this.visitService = visitService;
    }

    @Scheduled(cron = "${locavite.visits.reminder-cron:0 0 9 * * *}")
    public int sendDailyReminders() {
        return sendRemindersForDate(LocalDate.now().plusDays(1));
    }

    public int sendRemindersForDate(LocalDate targetDate) {
        return visitService.sendReminders(targetDate);
    }
}
