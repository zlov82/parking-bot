package ru.parking.parking_bot.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.parking.parking_bot.dto.BotResponse;
import ru.parking.parking_bot.service.BackendService;
import ru.parking.parking_bot.utils.InlineKeyboardFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InfoHandler implements CommandHandler {

    @Value("${telegram.bot.cleaning-free}")
    private Boolean isShowCleaningFree;

    private final BackendService backendService;
    private final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Override
    public boolean supports(String command) {
        return command.startsWith("/info");
    }

    @Override
    public BotResponse handle(Update update) {
        Long chatId = update.getMessage().getFrom().getId();
        log.debug("Command /info from user {}", chatId);

        List<Integer> userParkings = backendService.getParkingByUser(chatId);
        if (userParkings.isEmpty()) {
            return BotResponse.builder()
                    .chatId(chatId.toString())
                    .text("У вас нет привязанных парковочных мест.\n" +
                            "Для начала необходимо привязать в боте машиноместо")
                    .keyboard(addParkingKeyboard())
                    .build();
        }

        //Получаем ближайшие уборки по привязанным ММ
        Optional<Map<String, List<LocalDate>>> cleanings = backendService.getSchedulersCleaning(chatId);
        if (cleanings.isEmpty()) {
            String response = this.getFinalText(new StringBuilder("Не нашёл ближайших уборок ваших машиномест\n"));
            return BotResponse.builder()
                    .chatId(chatId.toString())
                    .text(response)
                    .parseMode("HTML")
                    .build();
        }

        log.debug("Receive cleaning plan:\n{}", cleanings);
        StringBuilder cleaningsText = new StringBuilder(getHtml(cleanings.get()));
        String response = this.getFinalText(cleaningsText);

        return BotResponse.builder()
                .chatId(chatId.toString())
                .text(cleanings.get().toString())
                .text(response)
                .parseMode("HTML")
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

    private String getCleaningFree(List<LocalDate> cleaningsFree) {
        StringBuilder html = new StringBuilder("<b>🧹 Уборка свободных машиномест</b>\n");
        cleaningsFree.forEach(date -> {
            html.append("— ")
                    .append(date.format(formatter))
                    .append("\n");
        });

        return html.toString();
    }

    private String getHtml(Map<String, List<LocalDate>> data) {
        StringBuilder html = new StringBuilder("<b>🧹 Уборка Ваших машиномест</b>\n");

        data.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String place = entry.getKey();
                    List<LocalDate> dates = entry.getValue().stream()
                            .sorted()
                            .toList();

                    html.append("🚗 <b>Машиноместо №")
                            .append(place)
                            .append("</b>\n");

                    for (LocalDate date : dates) {
                        html.append("— ")
                                .append(date.format(formatter))
                                .append("\n");
                    }
                    html.append("\n");

                });

        return html.toString();
    }

    private String getFinalText(StringBuilder text) {
        if (isShowCleaningFree) {
            List<LocalDate> cleaningsFree = backendService.getCleaningsFree();
            if (!cleaningsFree.isEmpty()) {
                text.append("\n")
                        .append(this.getCleaningFree(cleaningsFree));
            }
        }
        return text.toString();
    }
}
