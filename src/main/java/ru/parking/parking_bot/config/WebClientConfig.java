package ru.parking.parking_bot.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    @Qualifier("telegramClient")
    public WebClient telegramWebClient() {
        return WebClient.builder()
                .baseUrl("https://api.telegram.org")
                .build();
    }

    @Bean
    @Qualifier("backendClient")
    public WebClient backendWebClient(
            @Value("${backend.api-key}") String apiKey,
            @Value("${backend.base-url}") String baseUrl
    ) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-API-KEY", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
