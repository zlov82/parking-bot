package ru.parking.parking_bot.handler;

import org.telegram.telegrambots.meta.api.objects.Update;
import ru.parking.parking_bot.dto.BotResponse;

public interface CommandHandler {
    boolean supports(String command);
    BotResponse handle(Update update);
}
