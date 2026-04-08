package ru.parking.parking_bot.utils;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.LocalDate;
import java.util.List;

public final class DefaultInlineKeyboards {

    public static InlineKeyboardMarkup deleteCleaningList(List<LocalDate> cleanings, String callbackData) {
        List<InlineKeyboardRow> rows = cleanings.stream()
                .map(date ->
                        InlineKeyboardFactory.row(
                                InlineKeyboardFactory.callbackButton(
                                        String.valueOf(date),
                                        callbackData + date
                                )
                        )
                )
                .toList();

        return InlineKeyboardFactory.keyboardFromRows(rows);
    }

    public static <T> InlineKeyboardMarkup selectValues(List<T> values, String callbackData) {
        List<InlineKeyboardRow> rows = values.stream()
                .map(value ->
                        InlineKeyboardFactory.row(
                                InlineKeyboardFactory.callbackButton(
                                        String.valueOf(value),
                                        callbackData + value
                                )
                        )
                )
                .toList();

        return InlineKeyboardFactory.keyboardFromRows(rows);
    }

}
