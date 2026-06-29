package com.firesafety.platform.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationBeans {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
