package ru.parking.parking_bot.router;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.parking.parking_bot.dto.BotResponse;
import ru.parking.parking_bot.handler.CallbackHandler;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CallbackRouter {
    private final List<CallbackHandler> handlers;

    public BotResponse route(CallbackQuery callback) {
        return handlers.stream()
                .filter(h -> h.canHandle(callback))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("No callback handler for " + callback.getData())
                )
                .handle(callback);
    }
}
