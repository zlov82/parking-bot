package ru.parking.parking_bot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Configuration
@ConditionalOnProperty(name = "proxy.enabled", havingValue = "true")
@RequiredArgsConstructor
public class LongPollingConfig {

    private final ProxyProperties proxy;

    @Bean
    public TelegramBotsLongPollingApplication telegramBotsApplication() {
        Proxy proxyObj = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxy.getHost(), proxy.getPort()));
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .proxy(proxyObj)
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                // Отключаем переиспользование соединений — прокси закрывает idle-соединения
                // тихо, и OkHttp зависает до readTimeout на мёртвом соединении
                .connectionPool(new ConnectionPool(0, 1, TimeUnit.NANOSECONDS));

        if (proxy.getUsername() != null && !proxy.getUsername().isBlank()) {
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    // getRequestorType() returns null when called from Java's SocksSocketImpl (JDK 21),
                    // so we cannot check for RequestorType.PROXY here
                    return new PasswordAuthentication(proxy.getUsername(), proxy.getPassword().toCharArray());
                }
            });
        }

        OkHttpClient client = builder.build();
        return new TelegramBotsLongPollingApplication(ObjectMapper::new, () -> client);
    }
}
