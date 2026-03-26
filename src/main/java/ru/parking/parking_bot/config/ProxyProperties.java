package ru.parking.parking_bot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "proxy")
public class ProxyProperties {

    private boolean enabled = false;
    private String host;
    private int port = 1080;
    private String username;
    private String password;
}
