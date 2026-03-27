package ru.parking.parking_bot.dto;

import lombok.Data;

@Data
public class TImage {
    private byte[] data;
    private String fileId;
    private String filePath;
    private Long userId;
    private Long chatId;
}
