package ru.parking.parking_bot.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.parking.parking_bot.dto.BotResponse;
import ru.parking.parking_bot.service.BackendService;
import ru.parking.parking_bot.service.FSMService;
import ru.parking.parking_bot.utils.InlineKeyboardFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FreeCleaningHandler implements CommandHandler {

    private final FSMService fsmService;
    private final BackendService backend;

    @Override
    public boolean supports(String command) {
        return command.startsWith("/freecleaning");
    }

    @Override
    public BotResponse handle(Update update) {
        String chatId = update.getMessage().getFrom().getId().toString();
        StringBuilder text = new StringBuilder();
        InlineKeyboardMarkup keyboard;
        log.info("Command /freecleaning from user {}", chatId);

        List<LocalDate> localDates = backend.getCleaningsFree();

        if (localDates.isEmpty()) {
            text.append("Нет дат уборок свободных машиномет! Самое время завести новую!");
            keyboard = freeCleaningAddKeyboard();
        } else {
            text.append("Даты уборки свободных ММ:");
            text.append(buildFreeParkingCleaningTable(localDates));
            keyboard = cleaningFreeManageKeyboard();
        }

        return BotResponse.builder()
                .chatId(chatId)
                .text(text.toString())
                .parseMode("HTML")
                .keyboard(keyboard)
                .build();

    }

    private InlineKeyboardMarkup freeCleaningAddKeyboard() {
        return InlineKeyboardFactory.keyboard(
                InlineKeyboardFactory.row(
                        InlineKeyboardFactory.callbackButton(
                                "➕ Добавить дату уборки", "FREE_СLEANING_ADD"
                        )
                )
        );
    }

    private String buildFreeParkingCleaningTable(List<LocalDate> dates) {
        log.debug("creating table for new free cleanings");
        StringBuilder sb = new StringBuilder("\n");

        for (LocalDate date : dates) {
            sb.append("— ");
            sb.append(String.format(
                    date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            ));
            sb.append("\n");
        }

        return sb.toString();
    }

    private InlineKeyboardMarkup cleaningFreeManageKeyboard() {
        return InlineKeyboardFactory.keyboard(
                InlineKeyboardFactory.row(
                        InlineKeyboardFactory.callbackButton(
                                "➕ Добавить уборку свободных ММ", "FREE_СLEANING_ADD"
                        )
                ),
                InlineKeyboardFactory.row(
                        InlineKeyboardFactory.callbackButton(
                                "➖Удалить запись об уборке", "FREE_СLEANING_DELETE"
                        )
                )
        );
    }
}
