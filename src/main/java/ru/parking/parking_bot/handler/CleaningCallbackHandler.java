package ru.parking.parking_bot.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import ru.parking.parking_bot.client.TelegramApiClient;
import ru.parking.parking_bot.dto.BotResponse;
import ru.parking.parking_bot.service.BackendService;
import ru.parking.parking_bot.service.FSMService;
import ru.parking.parking_bot.utils.InlineKeyboardFactory;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleaningCallbackHandler implements CallbackHandler {

    private final TelegramApiClient telegramApi;
    private final FSMService fsmService;
    private final BackendService backendService;

    @Override
    public boolean canHandle(CallbackQuery callback) {
        boolean status = callback.getData().startsWith("СLEANING_");
        return status;
    }

    @Override
    public BotResponse handle(CallbackQuery callback) {
        Long chatId = callback.getFrom().getId();
        String data = callback.getData();
        telegramApi.answerCallback(callback.getId(), "Обработка...");

        if ("СLEANING_ADD".equals(data)) {
            fsmService.setState(chatId, FSMService.State.CLEANING_DATE_WAITING);
            return BotResponse.builder()
                    .text("Введите дату уборки в формате ДД.ММ.ГГГГ")
                    .chatId(chatId.toString())
                    .build();
        }

        if ("СLEANING_DELETE".equals(data)) {
            List<LocalDate> cleaningsDates = backendService.getNextCleaningsDates(10);
            return BotResponse.builder()
                    .text("Выберите дату уборку, которую следует удалить")
                    .chatId(chatId.toString())
                    .keyboard(deleteCleaningList(cleaningsDates))
                    .build();
        }

        if (data.startsWith("СLEANING_DELETE:")) {
            String value = data.substring("CLEANING_DELETE:".length());
            log.info("User {} wants to delete cleaning date {}", chatId, value);
            try {
                LocalDate cleaningDate = LocalDate.parse(value);
                boolean success = backendService.deleteCleaningPlan(cleaningDate);
                if (success) {
                    return BotResponse.builder()
                            .chatId(chatId.toString())
                            .text("План уборки на " + cleaningDate + " удален")
                            .build();
                } else {
                    return BotResponse.builder()
                            .chatId(chatId.toString())
                            .text("Не удалось удалить дату уборки")
                            .build();
                }
            } catch (RuntimeException e) {
                log.warn("failed to parse date {}", e.getMessage());
                return BotResponse.builder()
                        .chatId(chatId.toString())
                        .text("Не удалось удалить дату уборки")
                        .build();
            }
        }
        throw new IllegalStateException("Unsupported callback " + data);
    }

    private InlineKeyboardMarkup deleteCleaningList(List<LocalDate> cleanings) {
        List<InlineKeyboardRow> rows = cleanings.stream()
                .map(date ->
                        InlineKeyboardFactory.row(
                                InlineKeyboardFactory.callbackButton(
                                        String.valueOf(date),
                                        "СLEANING_DELETE:" + date
                                )
                        )
                )
                .toList();

        return InlineKeyboardFactory.keyboardFromRows(rows);
    }
}
