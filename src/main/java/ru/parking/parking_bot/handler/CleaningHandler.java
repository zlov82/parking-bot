package ru.parking.parking_bot.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.parking.parking_bot.dto.BotResponse;
import ru.parking.parking_bot.dto.CleaningPlanResponse;
import ru.parking.parking_bot.service.BackendService;
import ru.parking.parking_bot.service.FSMService;
import ru.parking.parking_bot.utils.InlineKeyboardFactory;

import java.time.format.DateTimeFormatter;
import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class CleaningHandler implements CommandHandler{

    private final FSMService fsmService;
    private final BackendService backendService;

    @Override
    public boolean supports(String command) {
        return command.startsWith("/cleaning");
    }

    @Override
    public BotResponse handle(Update update) {
        String chatId = update.getMessage().getFrom().getId().toString();
        StringBuilder text = new StringBuilder();
        InlineKeyboardMarkup keyboard;
        log.info("Command /cleaning from user {}", chatId);

        //Получаем последние уборки
        List<CleaningPlanResponse> nextCleaningPlans = backendService.getNextCleanings(20);

        if (nextCleaningPlans.isEmpty()) {
            text.append("Ой, не нашёл будущие уборки! Самое время завести новую!");
            keyboard = cleaningAddKeyboard();
        } else {
            text.append("Список будущих уборок, заведенных в боте:");
            text.append(buildParkingCleaningTable(nextCleaningPlans));
            text.append("При заведении уборки на существующую дату из списка - данные будут обновлены.\n");
            text.append("В одну дату может быть уборка только одного диапазона парковочных мест");
            keyboard = cleaningManageKeyboard();
        }

        return BotResponse.builder()
                .chatId(chatId)
                .text(text.toString())
                .parseMode("HTML")
                .keyboard(keyboard)
                .build();
    }

    private InlineKeyboardMarkup cleaningManageKeyboard() {
        return InlineKeyboardFactory.keyboard(
                InlineKeyboardFactory.row(
                        InlineKeyboardFactory.callbackButton(
                                "➕ Добавить дату уборки", "СLEANING_ADD"
                        )
                ),
                InlineKeyboardFactory.row(
                        InlineKeyboardFactory.callbackButton(
                                "➖Удалить запись об уборке", "СLEANING_DELETE"
                        )
                )
        );
    }

        private InlineKeyboardMarkup cleaningAddKeyboard() {
            return InlineKeyboardFactory.keyboard(
                    InlineKeyboardFactory.row(
                            InlineKeyboardFactory.callbackButton(
                                    "➕ Добавить дату уборки", "СLEANING_ADD"
                            )
                    )
            );
    }

    //todo переделать на уборки с парковочными местами
    private String buildParkingCleaningTable(List<CleaningPlanResponse> data) {
        log.debug("creating table for new cleanings");
        StringBuilder sb = new StringBuilder();

        sb.append("<pre>");
        sb.append(String.format(
                "%-12s |%-8s |%-8s %n",
                "Дата уборки", "c номера", "по номер"
        ));
        sb.append("-------------+---------+---------\n");

        for (CleaningPlanResponse item : data) {
            sb.append(String.format(
                    "%-12s | %-8d| %-8d%n",
                    item.getCleaningDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    item.getStartParkingNumber(),
                    item.getEndParkingNumber()
            ));
        }

        sb.append("</pre>");
        return sb.toString();
    }

}
