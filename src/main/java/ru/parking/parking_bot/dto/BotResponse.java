package ru.parking.parking_bot.dto;

import lombok.Builder;
import lombok.Data;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Data
@Builder
public class BotResponse {
    private String chatId;
    private String text;
    private InlineKeyboardMarkup keyboard;

    @Builder.Default
    private String parseMode = null;
}
