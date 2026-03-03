package ru.parking.parking_bot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.parking.parking_bot.client.TelegramApiClient;

@Service
@RequiredArgsConstructor
public class UserGroupValidator {
    private final TelegramApiClient telegramApi;

    @Value("${telegram.bot.private-group-id}")
    private String groupId;

    public boolean isUserInGroup(Long userId) {

        return telegramApi.isUserInGroup(userId,groupId);

        //todo добавить кэш
    }

}
