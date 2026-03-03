package ru.parking.parking_bot.dto;

public enum SendStatus {
    OK,
    RETRYABLE_ERROR,   // 400
    BLOCKED,           // 420 (наш маппинг 403 Telegram)
    INTERNAL_ERROR
}
