package ru.parking.parking_bot.handler;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.parking.parking_bot.dto.BotResponse;

public interface CallbackHandler {
    boolean canHandle(CallbackQuery callback);
    BotResponse handle(CallbackQuery callback);
}
