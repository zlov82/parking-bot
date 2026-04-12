package ru.parking.parking_bot.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.parking.parking_bot.dto.BotResponse;
import ru.parking.parking_bot.dto.HomelessStatistics;
import ru.parking.parking_bot.service.BackendService;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class HomelessStatHandler implements CommandHandler {

    private final BackendService backendService;

    @Override
    public boolean supports(String command) {
        return command.startsWith("/homeless");
    }

    @Override
    public BotResponse handle(Update update) {
        Long chatId = update.getMessage().getFrom().getId();
        log.debug("Command /homeless from user {}", chatId);

        Optional<HomelessStatistics> optional = backendService.getHomelessStatistics();
        if (optional.isEmpty()) {
            return BotResponse.builder()
                    .chatId(chatId.toString())
                    .text("Не удалось получить статистику. Попробуйте позже.")
                    .build();
        }

        return BotResponse.builder()
                .chatId(chatId.toString())
                .text(buildMessage(optional.get()))
                .parseMode("HTML")
                .build();
    }

    private String buildMessage(HomelessStatistics stats) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>🚗 Статистика автобомжей</b>");
        sb.append(" <i>(").append(stats.getPeriodText()).append(")</i>\n\n");

        sb.append("📊 Загружено: <b>").append(stats.getCount()).append("</b>\n");
        sb.append("🔑 Уникальных номеров: <b>").append(stats.getUniquePlates()).append("</b>\n");

        if (stats.getDailyUploads() != null && !stats.getDailyUploads().isEmpty()) {
            sb.append("\n📅 <b>По дням:</b>\n");
            stats.getDailyUploads().forEach(d ->
                    sb.append("— ").append(d.getDate())
                      .append(": <b>").append(d.getCount()).append("</b>\n")
            );
        }

        if (stats.getTopPlates() != null && !stats.getTopPlates().isEmpty()) {
            sb.append("\n🏆 <b>Топ номеров:</b>\n");
            int[] index = {1};
            stats.getTopPlates().forEach(p ->
                    sb.append(index[0]++).append(". <code>").append(p.getPlateNumber())
                      .append("</code> — ").append(p.getCount()).append(" раз\n")
            );
        }

        return sb.toString();
    }
}
