package ru.parking.parking_bot.router;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.parking.parking_bot.dto.BotResponse;
import ru.parking.parking_bot.handler.CommandHandler;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommandRouter {

    private final List<CommandHandler> handlers;

    public BotResponse route(Update update) {
        String command = update.getMessage().getText();
        return handlers.stream()
                .filter(h -> h.supports(command))
                .findFirst()
                .map(h -> h.handle(update))
                .orElse(BotResponse.builder()
                        .chatId(update.getMessage().getFrom().getId().toString())
                        .text("Команда не распознана")
                        .build());
    }
}
