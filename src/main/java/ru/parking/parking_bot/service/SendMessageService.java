package ru.parking.parking_bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.parking.parking_bot.client.TelegramApiClient;
import ru.parking.parking_bot.dto.SendMessageRequest;
import ru.parking.parking_bot.dto.SendMessageResponse;
import ru.parking.parking_bot.dto.SendStatus;
import ru.parking.parking_bot.dto.telegramApi.Message;
import ru.parking.parking_bot.dto.telegramApi.TelegramResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendMessageService {
    private final TelegramApiClient telegram;

    public SendMessageResponse sendMessageToUser(SendMessageRequest request) {
        TelegramResponse<Message> response = telegram.sendMessage(request.getChatId(), request.getMessage());

        if (response == null) {
            log.error("Telegram response is null (network or 5xx error)");
            return new SendMessageResponse(
                    SendStatus.INTERNAL_ERROR,
                    null,
                    "Telegram unavailable");
        }

        if (!response.isOk()) {
            Integer code = response.getError_code();
            String description = response.getDescription();
            log.warn("Telegram error {}: {}", code, description);

            // Пользователь заблокировал бота → Telegram 403
            if (code == 403) {
                return new SendMessageResponse(
                        SendStatus.BLOCKED,
                        code,
                        description
                );
            }

            if (code == 400 || code == 429) {
                return new SendMessageResponse(
                        SendStatus.RETRYABLE_ERROR,
                        code,
                        description);
            }

            return new SendMessageResponse(
                    SendStatus.INTERNAL_ERROR,
                    code,
                    description);
        }

        log.info("Message sent successes. id={}", response.getResult().getMessage_id());
        return new SendMessageResponse(
                SendStatus.OK,
                null,
                response.getResult().getMessage_id().toString());
    }
}
