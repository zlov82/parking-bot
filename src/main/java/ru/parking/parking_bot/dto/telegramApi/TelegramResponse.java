package ru.parking.parking_bot.dto.telegramApi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelegramResponse<T> {
    public boolean ok;

    // Успешный результат
    public T result;

    // Ошибка
    public Integer error_code;
    public String description;

    // Доп. параметры (rate limit, migrate)
    public ResponseParameters parameters;
}
