package ru.parking.parking_bot.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import ru.parking.parking_bot.client.TelegramApiClient;
import ru.parking.parking_bot.config.BotConfig;
import ru.parking.parking_bot.config.BotReplyService;
import ru.parking.parking_bot.dto.BotResponse;
import ru.parking.parking_bot.router.CallbackRouter;
import ru.parking.parking_bot.router.CommandRouter;
import ru.parking.parking_bot.service.FSMService;
import ru.parking.parking_bot.service.HomelessService;
import ru.parking.parking_bot.service.UserGroupValidator;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParkingBot implements SpringLongPollingBot, LongPollingUpdateConsumer {

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    private final BotConfig config;
    private final CommandRouter commandRouter;
    private final CallbackRouter callbackRouter;
    private final UserGroupValidator userValidator;
    private final FSMService userStateService;
    private final BotReplyService botReply;
    private final TelegramApiClient telegramApiClient;
    private final HomelessService homelessService;

    @Value("${telegram.bot.check-user-group}")
    private Boolean isCheckGroup;

    @Override
    public String getBotToken() {
        return config.getBotToken();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(List<Update> updates) {
        updates.forEach(update -> executor.submit(() -> consume(update)));
    }

    private void consume(Update update) {
        if (update.hasMessage()) {
            handleMessage(update);
        } else if (update.hasCallbackQuery()) {
            handleCallback(update);
        }
    }

    private void handleMessage(Update update) {
        logIncomingMessage(update);
        Long userId = update.getMessage().getFrom().getId();

        if (isCheckGroup) {
            if (!userValidator.isUserInGroup(userId)) {
                log.warn("Authorization FAIL userId {}", userId);
                telegramApiClient.sendFullMessage(
                        SendMessage.builder()
                                .chatId(userId)
                                .text("❌ Вы не состоите в группе.")
                                .build()
                );
                return;
            }
        }

        if (update.getMessage().hasPhoto()) {
            log.debug("Receive photo from user {} in chat {}", userId, update.getMessage().getChatId());
            BotResponse response = homelessService.processNewHomeless(update);
            logOutgoingMessage(response);
            telegramApiClient.sendFullMessage(toSendMessage(response));
            return;
        }

        if (update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            String userName = update.getMessage().getFrom().getUserName();
            Long chatId = update.getMessage().getChatId();

            if (!userId.equals(chatId) && !text.startsWith("/")) {
                log.debug("Message to bot from Chat without command - skip");
                telegramApiClient.sendFullMessage(
                        SendMessage.builder()
                                .chatId(chatId)
                                .text(botReply.getRandomReply())
                                .build()
                );
                return;
            }

            if (userStateService.hasState(userId)) {
                BotResponse response = userStateService.handleText(userId, text, userName);
                logOutgoingMessage(response);
                telegramApiClient.sendFullMessage(toSendMessage(response));
                return;
            }

            BotResponse response = commandRouter.route(update);
            logOutgoingMessage(response);
            telegramApiClient.sendFullMessage(toSendMessage(response));
        }
    }

    private void handleCallback(Update update) {
        var callback = update.getCallbackQuery();
        Long userId = callback.getFrom().getId();
        String data = callback.getData();
        log.info("Inbox callback from userId={} data={}", userId, data);

        BotResponse response = callbackRouter.route(callback);
        logOutgoingMessage(response);
        telegramApiClient.sendFullMessage(toSendMessage(response));
    }

    private SendMessage toSendMessage(BotResponse response) {
        return SendMessage.builder()
                .chatId(response.getChatId())
                .text(response.getText())
                .replyMarkup(response.getKeyboard())
                .parseMode(response.getParseMode())
                .build();
    }

    private void logIncomingMessage(Update update) {
        var message = update.getMessage();
        String login = message.getFrom() != null ? message.getFrom().getUserName() : "unknown";
        String chatId = message.getChatId().toString();
        String userId = message.getFrom().getId().toString();
        String text = message.hasText() ? message.getText() : "<no text>";
        log.info("Inbox message from {} (userId={} chatId={}) text={}", login, userId, chatId, text);
    }

    private void logOutgoingMessage(BotResponse response) {
        log.info("Outbox message to chatId={} text={}", response.getChatId(), response.getText());
    }
}
