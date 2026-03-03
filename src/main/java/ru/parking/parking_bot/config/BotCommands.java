package ru.parking.parking_bot.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.parking.parking_bot.client.TelegramApiClient;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class BotCommands {

    private final TelegramApiClient telegramApiClient;

    @PostConstruct
    public void setupCommands() {
        // команды для лички
        Map<String, Object> privatePayload = Map.of(
                "commands", List.of(
                        Map.of("command", "/info", "description", "Ближайшие даты уборки моих ММ"),
                        Map.of("command", "/parking", "description", "Управление моими ММ"),
                        Map.of("command", "/cleaning", "description", "Управление уборками")
                ),
                "scope", Map.of("type", "all_private_chats")
        );

        telegramApiClient.setMyCommands(privatePayload);

        // команды для групп
        Map<String, Object> groupPayload = Map.of(
                "commands", List.of(
                        Map.of("command", "/info", "description", "ближайшие даты уборки моих ММ")
                ),
                "scope", Map.of("type", "all_group_chats")
        );

        telegramApiClient.setMyCommands(groupPayload);
    }
}
