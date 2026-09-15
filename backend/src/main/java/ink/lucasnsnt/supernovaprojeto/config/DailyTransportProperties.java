package ink.lucasnsnt.supernovaprojeto.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.transport")
public class DailyTransportProperties {

    private ZoneId zoneId = ZoneId.of("America/Bahia");
    private LocalTime earlyDepartureThreshold = LocalTime.of(9, 0);
    private LocalTime previousDayConfirmationTime = LocalTime.of(20, 0);
    private LocalTime sameDayConfirmationTime = LocalTime.of(6, 0);
    private Duration responseDeadlineLead = Duration.ofHours(1);
    private Duration preliminaryOutboundLead = Duration.ofHours(1);
    private Duration departureChangeLock = Duration.ofMinutes(30);
    private Duration maximumDepartureAdjustment = Duration.ofMinutes(30);
    private Duration maximumReturnWait = Duration.ofMinutes(30);
    private Google google = new Google();
    private Geocoding geocoding = new Geocoding();

    @Getter
    @Setter
    public static class Geocoding {
        private boolean enabled;
        private String apiKey;
        private String endpoint = "https://maps.googleapis.com";
    }

    @Getter
    @Setter
    public static class Google {
        private boolean enabled;
        private String projectId;
        private String endpoint = "https://routeoptimization.googleapis.com";
    }
}
