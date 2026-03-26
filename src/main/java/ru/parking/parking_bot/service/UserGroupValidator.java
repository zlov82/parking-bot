package ru.parking.parking_bot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.parking.parking_bot.client.TelegramApiClient;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class UserGroupValidator {
    private final TelegramApiClient telegramApi;

    @Value("${telegram.bot.private-group-id}")
    private String groupId;

    private record CacheEntry(boolean inGroup, Instant expiresAt) {}
    private final Map<Long, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final java.time.Duration TTL = java.time.Duration.ofMinutes(5);

    public boolean isUserInGroup(Long userId) {
        CacheEntry entry = cache.get(userId);
        if (entry != null && Instant.now().isBefore(entry.expiresAt())) {
            return entry.inGroup();
        }
        boolean result = telegramApi.isUserInGroup(userId, groupId);
        cache.put(userId, new CacheEntry(result, Instant.now().plus(TTL)));
        return result;
    }
}
