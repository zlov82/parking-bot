package ru.parking.parking_bot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.parking.parking_bot.config.BotReplyService;
import ru.parking.parking_bot.dto.SendMessageRequest;
import ru.parking.parking_bot.dto.SendMessageResponse;
import ru.parking.parking_bot.service.SendMessageService;

@Slf4j
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class SendMessageController {

    private final SendMessageService service;

    @Value("${bot.api-key}")
    private String botKey;

    @PostMapping("/send")
    public ResponseEntity<SendMessageResponse> sendMessage(@RequestBody @Valid SendMessageRequest request,
                                                           @RequestHeader(value = "X-API-KEY", required = false) String apiKey) {
        log.info(request.toString());
        if (!botKey.equals(apiKey)) {
            log.debug("Invalid API KEY {}", apiKey);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new SendMessageResponse(null, null, "Invalid API KEY"));
        }
        SendMessageResponse response = service.sendMessageToUser(request);

        return switch (response.getStatus()) {
            case OK -> ResponseEntity.status(HttpStatus.OK)
                    .body(response);

            case RETRYABLE_ERROR -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(response);

            case BLOCKED -> ResponseEntity.status(HttpStatus.DESTINATION_LOCKED)
                    .body(response);

            case INTERNAL_ERROR -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        };
    }

}
