package ru.parking.parking_bot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.parking.parking_bot.config.BotConfig;
import ru.parking.parking_bot.config.BotReplyService;
import ru.parking.parking_bot.dto.BotResponse;
import ru.parking.parking_bot.router.CallbackRouter;
import ru.parking.parking_bot.router.CommandRouter;
import ru.parking.parking_bot.service.FSMService;
import ru.parking.parking_bot.service.UserGroupValidator;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TelegramWebhookController {

    private final BotConfig config;
    private final CommandRouter commandRouter;
    private final CallbackRouter callbackRouter;
    private final UserGroupValidator userValidator;
    private final FSMService userStateService;
    private final BotReplyService botReply;

    @Value("${telegram.bot.check-user-group}")
    private Boolean isCheckGroup;

    @PostMapping("${telegram.bot.webhook-path}")
    public ResponseEntity<?> onUpdateReceived(@RequestBody Update update) {
        if (update.hasMessage()) {
            return handleMessage(update);
        }

        if (update.hasCallbackQuery()) {
            return handleCallback(update);
        }
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<?> handleMessage(Update update) {
        logIncomingMessage(update);
        Long userId = update.getMessage().getFrom().getId();

        //проверка на вхождение в группу
        if (isCheckGroup) {
            if (!userValidator.isUserInGroup(userId)) {
                log.warn("Authorization FAIL userId {}", userId);
                return ResponseEntity.ok(
                        SendMessage.builder()
                                .chatId(userId)
                                .text("❌ Вы не состоите в группе.")
                                .build()
                );
            }
        }


        if (update.getMessage().hasText()) {
            //если ждем от пользователя сообщения:
            String text = update.getMessage().getText();
            String userName = update.getMessage().getFrom().getUserName();

            // Сообщение адресовано боту в групповом чате
            // и не начинается с команды
            Long chatId = update.getMessage().getChatId();
            if (!userId.equals(chatId) && !text.startsWith("/")) {
                log.debug("Message to bot from Chat without command - skip");
                return ResponseEntity.ok(
                        SendMessage.builder()
                                .chatId(chatId)
                                .text(botReply.getRandomReply())
                                .build()
                );

            }

            if (userStateService.hasState(userId)) {
                BotResponse response = userStateService.handleText(userId, text, userName);
                logOutgoingMessage(response);
                return ResponseEntity.ok(SendMessage.builder()
                        .chatId(response.getChatId())
                        .text(response.getText())
                        .replyMarkup(response.getKeyboard())
                        .parseMode(response.getParseMode())
                        .build());
            }

            BotResponse response = commandRouter.route(update);
            logOutgoingMessage(response);
            return ResponseEntity.ok(
                    SendMessage.builder()
                            .chatId(response.getChatId())
                            .text(response.getText())
                            .replyMarkup(response.getKeyboard())
                            .parseMode(response.getParseMode())
                            .build()
            );
        }
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<?> handleCallback(Update update) {
        var callback = update.getCallbackQuery();

        Long userId = callback.getFrom().getId();
        String data = callback.getData();

        log.info("Inbox callback from userId={} data={}", userId, data);

        BotResponse response = callbackRouter.route(callback);
        logOutgoingMessage(response);
        return ResponseEntity.ok(
                SendMessage.builder()
                        .chatId(response.getChatId())
                        .text(response.getText())
                        .replyMarkup(response.getKeyboard())
                        .parseMode(response.getParseMode())
                        .build()
        );
    }

    private void logIncomingMessage(Update update) {
        var message = update.getMessage();
        String login = message.getFrom() != null ? message.getFrom().getUserName() : "unknown";
        String chatId = message.getChatId().toString();
        String userId = message.getFrom().getId().toString();
        String text = message.hasText() ? message.getText() : "<no text>";

        log.info("Inbox message from {} (userId={} chatId={}) text={}", login, userId, chatId, text);
        //log.debug(update.toString());
    }

    private void logOutgoingMessage(BotResponse response) {
        log.info("Outbox message to chatId={} text={}", response.getChatId(), response.getText());
    }
}