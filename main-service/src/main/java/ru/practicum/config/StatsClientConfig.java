package ru.practicum.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.StatsClient;

@Configuration
public class StatsClientConfig {

    @Value("${stats.server.url}")
    private String statsServerUrl;

    @Bean
    public StatsClient statsClient() {
        return new StatsClient(statsServerUrl);
    }
}