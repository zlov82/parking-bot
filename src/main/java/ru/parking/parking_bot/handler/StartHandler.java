package ru.parking.parking_bot.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.parking.parking_bot.dto.BotResponse;

@Component
@RequiredArgsConstructor
public class StartHandler implements CommandHandler{

    @Value("${bot.start-message}")
    private String startMessage;

    @Override
    public boolean supports(String command) {
        return command.startsWith("/start");
    }

    @Override
    public BotResponse handle(Update update) {

        return BotResponse.builder()
                .chatId(update.getMessage().getFrom().getId().toString())
                .text(startMessage)
                .parseMode("HTML")
                .build();
    }
}
