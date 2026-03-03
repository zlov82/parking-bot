package ru.parking.parking_bot.utils;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.List;

public final class InlineKeyboardFactory {

    private InlineKeyboardFactory() {
    }

    /* ---------- кнопка ---------- */
    public static InlineKeyboardButton callbackButton(String text, String callbackData) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();
    }

    public static InlineKeyboardButton urlButton(String text, String url) {
        return InlineKeyboardButton.builder()
                .text(text)
                .url(url)
                .build();
    }

    /* ---------- строка ---------- */
    public static InlineKeyboardRow row(InlineKeyboardButton... buttons) {
        InlineKeyboardRow row = new InlineKeyboardRow();
        row.addAll(List.of(buttons));
        return row;
    }

    /* ---------- клавиатура ---------- */
    public static InlineKeyboardMarkup keyboard(InlineKeyboardRow... rows) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(rows))
                .build();
    }

    public static InlineKeyboardMarkup keyboardFromRows(List<InlineKeyboardRow> rows) {
        return InlineKeyboardMarkup.builder()
                .keyboard(rows)
                .build();
    }
}