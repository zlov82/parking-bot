package ru.parking.parking_bot.handler;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import ru.parking.parking_bot.dto.BotResponse;
import ru.parking.parking_bot.service.BackendService;
import ru.parking.parking_bot.service.FSMService;
import ru.parking.parking_bot.client.TelegramApiClient;
import ru.parking.parking_bot.utils.InlineKeyboardFactory;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParkingCallbackHandler implements CallbackHandler {

    private final BackendService backClient;
    private final FSMService userStateService;
    private final TelegramApiClient telegramApi;

    @Override
    public boolean canHandle(CallbackQuery callback) {
        return callback.getData().startsWith("PARKING_");
    }

    @Override
    public BotResponse handle(CallbackQuery callback) {
        Long chatId = callback.getFrom().getId();
        String data = callback.getData();

        telegramApi.answerCallback(callback.getId(), "Обработка...");

        if ("PARKING_ADD".equals(data)) {
            userStateService.setState(chatId, FSMService.State.PARKING_ADD_WAITING);
            return BotResponse.builder()
                    .text("Введите номер парковочного места")
                    .chatId(chatId.toString())
                    .build();
        }

        if ("PARKING_DELETE".equals(data)) {
            List<Integer> parkings = backClient.getParkingByUser(chatId);

            return BotResponse.builder()
                    .chatId(chatId.toString())
                    .text("Выберите парковочное место для удаления")
                    .keyboard(deleteParkingKeyboard(parkings))
                    .build();
        }

        if (data.startsWith("PARKING_DELETE:")) {
            String parkingId = data.substring("PARKING_DELETE:".length());
            log.info("User {} delete parking {}",chatId,parkingId);

            backClient.deleteClientParking(chatId,Integer.valueOf(parkingId));
            return BotResponse.builder()
                    .chatId(chatId.toString())
                    .text("Парковочное место удалено ❌")
                    .build();
        }
        throw new IllegalStateException("Unsupported callback " + data);
    }

    private InlineKeyboardMarkup deleteParkingKeyboard(List<Integer> parkings) {
        List<InlineKeyboardRow> rows = parkings.stream()
                .map(number ->
                        InlineKeyboardFactory.row(
                                InlineKeyboardFactory.callbackButton(
                                        String.valueOf(number),
                                        "PARKING_DELETE:" + number
                                )
                        )
                )
                .toList();

        return InlineKeyboardFactory.keyboardFromRows(rows);
    }
}



