package com.sp.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {

    /** 시간에 기대는 로직을 테스트에서 앞당겨 볼 수 있게 빈으로 뺀다. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
