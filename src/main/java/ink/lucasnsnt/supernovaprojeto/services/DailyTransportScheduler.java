package ink.lucasnsnt.supernovaprojeto.services;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DailyTransportScheduler {

    private final DailyConfirmationService confirmationService;
    private final TripPlanningService tripPlanningService;
    private final Clock clock;

    @Scheduled(cron = "${app.transport.scheduler-cron:0 * * * * *}",
            zone = "${app.transport.zone-id:America/Bahia}")
    public void processConfirmations() {
        LocalDate today = LocalDate.now(clock);
        confirmationService.releaseAvailableForDate(today);
        confirmationService.releaseAvailableForDate(today.plusDays(1));
        confirmationService.expirePending();
        tripPlanningService.planReadyConfirmations();
    }
}
