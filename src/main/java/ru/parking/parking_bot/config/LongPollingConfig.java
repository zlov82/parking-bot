package ru.parking.parking_bot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

import java.net.InetSocketAddress;
import java.net.Proxy;

@Configuration
@ConditionalOnProperty(name = "proxy.enabled", havingValue = "true")
@RequiredArgsConstructor
public class LongPollingConfig {

    private final ProxyProperties proxy;

    @Bean
    public TelegramBotsLongPollingApplication telegramBotsApplication() {
        Proxy proxyObj = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxy.getHost(), proxy.getPort()));
        OkHttpClient.Builder builder = new OkHttpClient.Builder().proxy(proxyObj);

        if (proxy.getUsername() != null && !proxy.getUsername().isBlank()) {
            builder.proxyAuthenticator((route, response) ->
                    response.request().newBuilder()
                            .header("Proxy-Authorization", Credentials.basic(proxy.getUsername(), proxy.getPassword()))
                            .build()
            );
        }

        OkHttpClient client = builder.build();
        return new TelegramBotsLongPollingApplication(ObjectMapper::new, () -> client);
    }
}
