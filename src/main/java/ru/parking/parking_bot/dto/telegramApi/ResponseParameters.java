package ru.parking.parking_bot.dto.telegramApi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseParameters {
    // Если группа мигрировала в супергруппу
    public Long migrate_to_chat_id;

    // Если превышен rate limit
    public Integer retry_after;
}
