package ru.parking.parking_bot.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.parking.parking_bot.config.BotConfig;
import ru.parking.parking_bot.dto.telegramApi.Message;
import ru.parking.parking_bot.dto.telegramApi.TelegramResponse;

@Slf4j
@Service
public class TelegramApiClient {

    private final BotConfig config;
    private final WebClient webClient;

    public TelegramApiClient(
            @Qualifier("telegramClient")
            WebClient webClient,
            BotConfig config
    ) {
        this.webClient = webClient;
        this.config = config;
    }

    private String api(String path) {
        return UriComponentsBuilder
                .fromPath("/bot{token}" + path)
                .buildAndExpand(config.getBotToken())
                .toUriString();
    }

    // TelegramApi /getChatMember
    public boolean isUserInGroup(Long userId, String groupId) {
        try {
            TelegramChatMemberResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/bot{token}/getChatMember")
                            .queryParam("chat_id", groupId)
                            .queryParam("user_id", userId)
                            .build(config.getBotToken())
                    )
                    .retrieve()
                    .bodyToMono(TelegramChatMemberResponse.class)
                    .block();

            if (response != null && response.ok) {
                String status = response.result.status;
                return status.equals("member")
                        || status.equals("administrator")
                        || status.equals("creator");
            }
        } catch (Exception e) {
            log.warn("getChatMember failed userId={}", userId, e);
        }
        return false;
    }

    // Telegram API /sendMessage
    public TelegramResponse<Message> sendMessage(Long chatId, String text) {
        try {
            return webClient.post()
                    .uri(api("/sendMessage"))
                    .bodyValue(new SendMessageRequest(chatId, text))
                    .exchangeToMono(response -> response.bodyToMono(
                            new ParameterizedTypeReference<TelegramResponse<Message>>() {}))
                    .block();

        } catch (Exception e) {
            log.error("Telegram sendMessage failed chatId={}", chatId, e);
            return null;
        }
    }

    // Telegram API //answerCallbackQuery (ответ для телеграм, что бот принял callback)
    public void answerCallback(String callbackId, String text) {
        try {
            webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/bot{token}/answerCallbackQuery")
                            .build(config.getBotToken())
                    )
                    .bodyValue(new AnswerCallbackRequest(callbackId, text))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (Exception e) {
            log.warn("answerCallback failed callbackId={}", callbackId, e);
        }
    }

    // Telegram API /sendMessage (полный объект с клавиатурой и parseMode)
    public void sendFullMessage(SendMessage message) {
        try {
            webClient.post()
                    .uri(api("/sendMessage"))
                    .bodyValue(message)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (Exception e) {
            log.error("Telegram sendFullMessage failed chatId={}", message.getChatId(), e);
        }
    }

    public void setMyCommands(Object payload) {
        webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/bot{token}/setMyCommands")
                        .build(config.getBotToken()))
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe();
    }

    public String getFilePath(String fileId) {

        TelegramFileResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/bot{token}/getFile")
                        .queryParam("file_id", fileId)
                        .build(config.getBotToken()))
                .retrieve()
                .bodyToMono(TelegramFileResponse.class)
                .block();

        if (response != null && response.ok) {
            return response.result.file_path;
        }

        throw new RuntimeException("Failed to get file path");
    }

    public byte[] downloadFile(String fileId) {

        String filePath = getFilePath(fileId);
        log.info("Download start: filePath={}", filePath);

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                byte[] result = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/file/bot{token}/{filePath}")
                                .build(config.getBotToken(), filePath))
                        .retrieve()
                        .bodyToMono(byte[].class)
                        .block();
                log.info("Download success: attempt={} filePath={} bytes={}", attempt, filePath, result != null ? result.length : 0);
                return result;
            } catch (Exception e) {
                log.warn("Download attempt {} failed: filePath={}", attempt, filePath, e);
                if (attempt == 3) throw e;
            }
        }
        throw new IllegalStateException("unreachable");
    }


    // DTO
    public record SendMessageRequest(Long chat_id, String text) {}

    public record AnswerCallbackRequest(String callback_query_id, String text) {}

    public static class TelegramChatMemberResponse {
        public boolean ok;
        public ChatMember result;
    }

    public static class ChatMember {
        public String status;
    }

    public static class TelegramFileResponse {
        public boolean ok;
        public TelegramFile result;
    }

    public static class TelegramFile {
        public String file_id;
        public String file_path;
    }

}
