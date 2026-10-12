package org.altechego.leavewise.common.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Lisbon");


    @Bean
    public Clock clock() {
        return Clock.system(BUSINESS_ZONE);
    }
}
