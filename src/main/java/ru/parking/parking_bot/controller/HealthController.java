package ru.parking.parking_bot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.parking.parking_bot.client.TelegramApiClient;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final TelegramApiClient telegramApiClient;

    @GetMapping
    public String health(@RequestHeader Map<String, String> headers) {
        log.info("GET /health");
        headers.forEach((key, value) -> log.debug("Header: {} = {}", key, value));
        return LocalDateTime.now() + " ALIVE!";
    }
}
