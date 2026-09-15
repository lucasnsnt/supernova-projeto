package ink.lucasnsnt.supernovaprojeto.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(DailyTransportProperties.class)
public class TimeConfig {

    @Bean
    public Clock applicationClock(DailyTransportProperties properties) {
        return Clock.system(properties.getZoneId());
    }
}
