package ru.parking.parking_bot.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    private ReactorClientHttpConnector buildConnector(ProxyProperties proxy) {
        if (!proxy.isEnabled()) {
            return new ReactorClientHttpConnector(HttpClient.create());
        }
        // Каждый запрос — новое соединение, без пула.
        // Duration.ZERO в maxIdleTime означает "без ограничения" (не "сразу вытеснять"),
        // поэтому ConnectionProvider.newConnection() — единственный способ гарантировать
        // что мёртвое прокси-соединение никогда не переиспользуется.
        HttpClient httpClient = HttpClient.newConnection()
                .protocol(HttpProtocol.HTTP11)
                .responseTimeout(Duration.ofMinutes(5))
                .proxy(spec -> {
                    var builder = spec.type(ProxyProvider.Proxy.SOCKS5)
                            .host(proxy.getHost())
                            .port(proxy.getPort());
                    if (proxy.getUsername() != null && !proxy.getUsername().isBlank()) {
                        builder.username(proxy.getUsername())
                                .password(u -> proxy.getPassword());
                    }
                });
        return new ReactorClientHttpConnector(httpClient);
    }

    @Bean
    @Qualifier("telegramClient")
    public WebClient telegramWebClient(ProxyProperties proxy) {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(config -> config.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
        return WebClient.builder()
                .baseUrl("https://api.telegram.org")
                .clientConnector(buildConnector(proxy))
                .exchangeStrategies(strategies)
                .build();
    }

    @Bean
    @Qualifier("backendClient")
    public WebClient backendWebClient(
            @Value("${backend.api-key}") String apiKey,
            @Value("${backend.base-url}") String baseUrl,
            ProxyProperties proxy
    ) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-API-KEY", apiKey)
                .defaultHeader("Content-Type", "application/json")
                //.clientConnector(buildConnector(proxy))
                .build();
    }
}
