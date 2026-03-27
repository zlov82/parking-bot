package ru.parking.parking_bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.parking.parking_bot.dto.BotResponse;
import ru.parking.parking_bot.dto.CheckPlateResponse;
import ru.parking.parking_bot.dto.TImage;
import ru.parking.parking_bot.utils.DefaultInlineKeyboards;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomelessService {

    private final MediaService mediaService;
    private final FSMService fsmService;

    public BotResponse processNewHomeless(Update update) {

        Long userId = update.getMessage().getFrom().getId();
        Long chatId = update.getMessage().getChatId();

        TImage image = mediaService.downloadBestPhoto(update);
        image.setUserId(userId);
        image.setChatId(chatId);

        log.debug("Image from telegram fileId {}, path {}:",image.getFileId(),image.getFilePath());

        Optional<CheckPlateResponse> optional = mediaService.getPlateById(image.getFileId());
        if (optional.isPresent()) {
            String plate = optional.get().getPlateNumber();
            return BotResponse.builder()
                    .chatId(userId.toString())
                    .text("Эту фотографию машины с номером " + plate + " уже загружали ранее")
                    .parseMode("HTML")
                    .build();
        }

        List<String> plates = mediaService.sendImageToBack(image);
        log.debug("Find plates count {}", plates.size());

        if (plates.isEmpty()) {
            return BotResponse.builder()
                    .chatId(userId.toString())
                    .text("Не удалось распознать номер автомобиля")
                    .build();
        }

        if (plates.size() == 1) {
            String plate = plates.getFirst();
            String fileId = image.getFileId();
            log.debug("File id to update {}", fileId);
            boolean isUpdate = mediaService.setPlate(fileId, plate);
            if (isUpdate) {
                return BotResponse.builder()
                        .chatId(userId.toString())
                        .text("В базу успешно занесен бомж с номером " + plate)
                        .build();
            }
        } else {
            fsmService.setState(userId, FSMService.State.HOMELESS_SELECT);
            fsmService.setFileId(userId, image.getFileId());
            return BotResponse.builder()
                    .chatId(userId.toString())
                    .text("На фото есть несколько номеров. Нужно выбрать один:")
                    .keyboard(DefaultInlineKeyboards.selectValues(plates,"HOMELESS_SELECT:"))
                    .build();
        }

        return BotResponse.builder()
                .chatId(userId.toString())
                .text("Что-то пошло не так...")
                .build();

    }
}
