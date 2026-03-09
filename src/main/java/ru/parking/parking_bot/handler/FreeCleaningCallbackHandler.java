package ru.parking.parking_bot.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.parking.parking_bot.client.TelegramApiClient;
import ru.parking.parking_bot.dto.BotResponse;
import ru.parking.parking_bot.service.BackendService;
import ru.parking.parking_bot.service.FSMService;

import java.time.LocalDate;
import java.util.List;

import static ru.parking.parking_bot.utils.DefaultInlineKeyboards.deleteCleaningList;

@Slf4j
@Component
@RequiredArgsConstructor
public class FreeCleaningCallbackHandler implements CallbackHandler {

    private final TelegramApiClient telegramApi;
    private final FSMService fsmService;
    private final BackendService backendService;

    @Override
    public boolean canHandle(CallbackQuery callback) {
        return callback.getData().startsWith("FREE_");
    }

    @Override
    public BotResponse handle(CallbackQuery callback) {
        Long chatId = callback.getFrom().getId();
        String data = callback.getData();
        telegramApi.answerCallback(callback.getId(), "Обработка...");

        if ("FREE_СLEANING_ADD".equals(data)) {
            fsmService.setState(chatId, FSMService.State.FREE_CLEANING_ADD_WAITING);
            return BotResponse.builder()
                    .text("Введите дату уборки в формате ДД.ММ.ГГГГ")
                    .chatId(chatId.toString())
                    .build();
        }

        if ("FREE_СLEANING_DELETE".equals(data)) {
            List<LocalDate> cleaningsDates = backendService.getCleaningsFree();
            return BotResponse.builder()
                    .text("Выберите дату уборку, которую следует удалить")
                    .chatId(chatId.toString())
                    .keyboard(deleteCleaningList(cleaningsDates, "FREE_CLEANING_DELETE:"))
                    .build();

        }

        if (data.startsWith("FREE_CLEANING_DELETE:")) {
            String value = data.substring("FREE_CLEANING_DELETE:".length());
            log.info("User {} wants to delete cleaning date {}", chatId, value);
            boolean isDeleted = backendService.deleteFreeCleaning(value);
            if (isDeleted) {
                return BotResponse.builder()
                        .chatId(chatId.toString())
                        .text("Дата уборки свободных ММ удалена ❌")
                        .build();
            } else {
                return BotResponse.builder()
                        .chatId(chatId.toString())
                        .text("Ошибка удаления")
                        .build();
            }

        }

        throw new IllegalStateException("Unsupported callback " + data);
    }
}
