package ru.parking.parking_bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.parking.parking_bot.dto.BotResponse;
import ru.parking.parking_bot.dto.CleaningPlan;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class FSMService {

    private final BackendService backendClient;
    private static final Duration STATE_TTL = Duration.ofSeconds(45);
    private final Map<Long, UserState> userStateDb = new ConcurrentHashMap<>();
    private final Map<Long, CleaningPlan> cleaningPlanDb = new ConcurrentHashMap<>();

    public record UserState(State state,
                            Instant updatedAt) {
    }

    public enum State {
        IDLE,
        PARKING_ADD_WAITING,
        CLEANING_DATE_WAITING,
        CLEANING_START_PARKING_WAITING,
        CLEANING_END_PARKING_WAITING,
        DELETE_CLEANING_WAITING
    }


    public BotResponse handleText(Long userId, String text, String userName) {
        State state = getState(userId);

        switch (state) {
            //пользователь добавляет парковку
            case PARKING_ADD_WAITING -> {
                try {
                    Integer paringNumber = Integer.parseInt(text);

                    if (paringNumber < 1 || paringNumber > 500) {
                        clearState(userId);
                        return BotResponse.builder()
                                .chatId(userId.toString())
                                .text("🤦 Приму только число в диапазоне от 1 до 500. Повторите")
                                .build();
                    }

                    boolean success = backendClient.bindParking(userId, paringNumber, userName);
                    clearState(userId);

                    if (!success) {
                        return BotResponse.builder()
                                .chatId(userId.toString())
                                .text("Не получилось. Попробуйте позже 🤦")
                                .build();
                    }
                    return BotResponse.builder()
                            .chatId(userId.toString())
                            .text("Парковочное место успешно привязано ✅")
                            .build();

                } catch (NumberFormatException e) {
                    log.warn("A numberic value was expected from client {}", e.getMessage());
                    return BotResponse.builder()
                            .chatId(userId.toString())
                            .text("🤦Это было точно не число, попробуйте ещё раз")
                            .build();
                }
            }

            //Заведение уборки - ШАГ Ввод даты
            case CLEANING_DATE_WAITING -> {
                try {
                    // Парсим введённый текст в LocalDate
                    DateTimeFormatter userFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                    LocalDate date = LocalDate.parse(text, userFormatter);
                    if (date.isBefore(LocalDate.now())) {
                        return BotResponse.builder()
                                .chatId(userId.toString())
                                .text("Дата не может быть в прошлом\nПовторите ввод даты:")
                                .build();

                    }

                    CleaningPlan plan = getPlan(userId);
                    plan.setDate(date);

                    setState(userId, State.CLEANING_START_PARKING_WAITING);

                    return BotResponse.builder()
                            .chatId(userId.toString())
                            .text("Введите номер начального парковочного места уборки")
                            .build();

                } catch (DateTimeParseException e) {
                    return BotResponse.builder()
                            .chatId(userId.toString())
                            .text("Пожалуйста, введите дату в формате ДД.ММ.ГГГГ")
                            .build();
                }
            }

            //Заведение уборки - ШАГ Ввод начального парковочного места
            case CLEANING_START_PARKING_WAITING -> {
                try {
                    int start = Integer.parseInt(text);
                    if (start <= 0 || start > 999) {
                        return BotResponse.builder()
                                .chatId(userId.toString())
                                .text("Введен некорректиный номер начального места\n" +
                                        "Повторите ввод начального парковочного места уборки")
                                .build();
                    }
                    CleaningPlan plan = getPlan(userId);
                    plan.setStartParking(start);

                    setState(userId, State.CLEANING_END_PARKING_WAITING);

                    return BotResponse.builder()
                            .chatId(userId.toString())
                            .text("Введите номер конечного парковочного места уборки")
                            .build();
                } catch (NumberFormatException e) {
                    return BotResponse.builder()
                            .chatId(userId.toString())
                            .text("Ой, кажется это бы не номер...\n" +
                                    "Введите корректный номер парковочного места")
                            .build();
                }
            }

            //Заведение уборки - ШАГ Ввода конечного рабочего места
            case CLEANING_END_PARKING_WAITING -> {
                try {
                    int end = Integer.parseInt(text);
                    CleaningPlan plan = getPlan(userId);
                    if (plan.getStartParking() > end || end > 999) {
                        return BotResponse.builder()
                                .chatId(userId.toString())
                                .text("Введен некорректиный номер\n" +
                                        "Повторите ввод последнего парковочного места уборки")
                                .build();
                    }

                    plan.setEndParking(end);

                    clearState(userId);
                    clearPlan(userId);

                    boolean sendSuccess = backendClient.scheduleCleaning(
                            plan.getDate().toString(),
                            plan.getStartParking(),
                            plan.getEndParking()
                    );

                    if (sendSuccess) {
                        return BotResponse.builder()
                                .chatId(userId.toString())
                                .text(String.format(
                                        "✅ Уборка запланирована:\nДата: %s\nМеста:\n с: %d\n по: %d",
                                        plan.getDate().toString(), plan.getStartParking(), plan.getEndParking()
                                ))
                                .build();
                    } else {
                        return BotResponse.builder()
                                .chatId(userId.toString())
                                .text("Что-то пошло не так. Попробуйте позже")
                                .build();
                    }
                } catch (NumberFormatException e) {
                    return BotResponse.builder()
                            .chatId(userId.toString())
                            .text("Ой, кажется вы ввели не число\n" +
                                    "Введите корректное число парковочного места")
                            .build();
                }
            }

        }
        throw new IllegalStateException("Unhandled state for text: " + state);
    }

    //
    public void setState(Long userId, State state) {
        userStateDb.put(userId, new UserState(state, Instant.now()));
        log.debug("FsmState for user {} is {}", userId, userStateDb.get(userId).toString());
    }

    public State getState(Long userId) {
        UserState userState = userStateDb.get(userId);

        if (userState == null) {
            return State.IDLE;
        }

        if (isExpired(userState)) {
            log.debug("User state IS EXPIRED: {}",userState.toString());
            userStateDb.remove(userId);
            return State.IDLE;
        }

        return userState.state();
    }

    public boolean hasState(Long userId) {
        if (getState(userId) != State.IDLE) {
            return true;
        }
        return false;
    }

    public void clearState(Long userId) {
        log.debug("User {} state is clearing",userId);
        userStateDb.remove(userId);
    }

    private boolean isExpired(UserState state) {
        return Instant.now().isAfter(
                state.updatedAt().plus(STATE_TTL)
        );
    }

    private CleaningPlan getPlan(Long userId) {
        return cleaningPlanDb.computeIfAbsent(userId, k -> new CleaningPlan());
    }

    private void clearPlan(Long userId) {
        cleaningPlanDb.remove(userId);
    }

}

