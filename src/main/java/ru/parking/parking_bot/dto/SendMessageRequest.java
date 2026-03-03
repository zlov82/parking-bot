package ru.parking.parking_bot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class SendMessageRequest {
    //@Positive
    private Long chatId;

    @NotBlank
    private String message;
}
