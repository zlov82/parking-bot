package ru.parking.parking_bot.dto.telegramApi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Chat {
    public Long id;
    public String type; // private, group, supergroup, channel
    public String title; // для групп/каналов
    public String username; // для публичных чатов
}
