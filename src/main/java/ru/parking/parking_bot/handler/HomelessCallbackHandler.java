package ru.parking.parking_bot.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.parking.parking_bot.client.TelegramApiClient;
import ru.parking.parking_bot.dto.BotResponse;
import ru.parking.parking_bot.service.BackendService;
import ru.parking.parking_bot.service.FSMService;
import ru.parking.parking_bot.service.MediaService;

@Slf4j
@Component
@RequiredArgsConstructor
public class HomelessCallbackHandler implements CallbackHandler {
    private final TelegramApiClient telegramApi;
    private final FSMService fsmService;
    private final BackendService backendService;
    private final MediaService mediaService;

    @Override
    public boolean canHandle(CallbackQuery callback) {
        return callback.getData().startsWith("HOMELESS");
    }

    @Override
    public BotResponse handle(CallbackQuery callback) {

        Long chatId = callback.getFrom().getId();
        String data = callback.getData();
        telegramApi.answerCallback(callback.getId(), "Обработка...");

        if (data.startsWith("HOMELESS_SELECT:")) {
            String value = data.substring("HOMELESS_SELECT:".length());
            log.info("User {} wants homeless plate {}", chatId, value);
            String fileId = fsmService.getFileId(chatId);
           log.debug("get FileId ib FSMService {}",fileId);

            if (mediaService.setPlate(fileId, value)) {
                fsmService.clearState(chatId);
                return BotResponse.builder()
                        .chatId(chatId.toString())
                        .text("Успешно добавлен номер " + value)
                        .build();
            }
            fsmService.clearState(chatId);
            return BotResponse.builder()
                    .chatId(chatId.toString())
                    .text("Не получилось")
                    .build();
        }


        return null;
    }
}
