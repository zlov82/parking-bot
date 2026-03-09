package ru.parking.parking_bot.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.parking.parking_bot.client.BackendApiClient;
import ru.parking.parking_bot.dto.CleaningPlanResponse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackendService {

    private final BackendApiClient client;

    public List<Integer> getParkingByUser(Long userId) {
        return client.getParkingByUser(userId);
    }

    public void deleteClientParking(Long userId, Integer parkingId) {
        client.deleteUserParking(userId, parkingId);
    }

    public boolean bindParking(Long userId, Integer paringNumber, String userName) {
        return client.bindParkingNumberToUser(userId, userName, paringNumber);
    }

    public Optional<Map<String, List<LocalDate>>> getSchedulersCleaning(Long chatId) {
        return client.getUserCleanings(chatId);
    }

    public boolean scheduleCleaning(String cleaningDate, Integer startParking, Integer endParking) {
        return client.createScheduleCleaning(cleaningDate, startParking, endParking);
    }

    public List<CleaningPlanResponse> getNextCleanings(int count) {

        return client.getNextCleaningPlans(count);

    }

    public List<LocalDate> getNextCleaningsDates(int count) {
        List<CleaningPlanResponse> cleaningPlan = getNextCleanings(count);

        return cleaningPlan.stream()
                .map(CleaningPlanResponse::getCleaningDate)
                .toList();
    }

    public boolean deleteCleaningPlan(LocalDate cleaningDate) {
        return client.deleteCleaningPlan(cleaningDate);
    }

    public List<LocalDate> getCleaningsFree() {
        return client.getFreeCleanings();
    }

    public boolean saveFreeCleanind(LocalDate date) {
        return client.createFreeCleaning(date);
    }

    public boolean deleteFreeCleaning(String value) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate date = LocalDate.parse(value, formatter);
            return client.deleteFreeCleaning(date);
        } catch (DateTimeParseException e) {
            log.warn("Error convert {} to LocalDate", value);
            return false;
        }
    }
}

