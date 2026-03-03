package ru.parking.parking_bot.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.parking.parking_bot.dto.BotResponse;
import ru.parking.parking_bot.service.BackendService;
import ru.parking.parking_bot.utils.InlineKeyboardFactory;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ParkingHandler implements CommandHandler {

    private final BackendService backClient;

    @Override
    public boolean supports(String command) {
        return command.startsWith("/parking");
    }

    @Override
    public BotResponse handle(Update update) {
        String chatId = update.getMessage().getFrom().getId().toString();

        List<Integer> parkings = backClient.getParkingByUser(Long.valueOf(chatId));

        if (parkings.isEmpty()) {
            return BotResponse.builder()
                    .chatId(chatId)
                    .text("У вас нет привязанных парковочных мест")
                    .keyboard(addParkingKeyboard())
                    .build();
        }

        StringBuilder parkingList = new StringBuilder("Ваши парковочные места:\n\n");
        for (int i = 0; i < parkings.size(); i++) {
            parkingList.append("🚗 ").append(parkings.get(i)).append("\n");
        }
        parkingList.append("\nЧто вы хотите сделать?");
        return BotResponse.builder()
                .chatId(chatId)
                .text(parkingList.toString())
                .keyboard(manageParkingKeyboard())
                .build();

    }

    private InlineKeyboardMarkup addParkingKeyboard() {
        return InlineKeyboardFactory.keyboard(
                InlineKeyboardFactory.row(
                        InlineKeyboardFactory.callbackButton(
                                "➕ Привязать парковочное место", "PARKING_ADD"
                        )
                )
        );
    }

    private InlineKeyboardMarkup manageParkingKeyboard() {
        return InlineKeyboardFactory.keyboard(
                InlineKeyboardFactory.row(
                        InlineKeyboardFactory.callbackButton(
                                "➕ Привязать парковочное место", "PARKING_ADD"
                        )
                ),
                InlineKeyboardFactory.row(
                        InlineKeyboardFactory.callbackButton(
                                "➖ Отвязать парковочное место", "PARKING_DELETE"
                        )
                )
        );
    }

}
