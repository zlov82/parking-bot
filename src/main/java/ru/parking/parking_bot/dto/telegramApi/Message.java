package ru.parking.parking_bot.dto.telegramApi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {
    public Long message_id;
    public Long date;
    public String text;

    public Chat chat;
}
