package ru.parking.parking_bot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.photo.PhotoSize;
import ru.parking.parking_bot.client.BackendApiClient;
import ru.parking.parking_bot.client.TelegramApiClient;
import ru.parking.parking_bot.dto.CheckPlateResponse;
import ru.parking.parking_bot.dto.TImage;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MediaService {
    private final TelegramApiClient telegramApi;
    private final BackendApiClient backend;

    public TImage downloadBestPhoto(Update update) {

        List<PhotoSize> photos = update.getMessage().getPhoto();

        if (photos == null || photos.isEmpty()) {
            throw new RuntimeException("Photo not found in update");
        }

        PhotoSize bestPhoto = photos.getLast();
        String fileId = bestPhoto.getFileId();

        TImage image = new TImage();
        image.setData(telegramApi.downloadFile(fileId));
        image.setFileId(fileId);
        image.setFilePath(bestPhoto.getFilePath());
        return image;
    }

    public List<String> sendImageToBack(TImage image) {
        return backend.uploadTImage(image);
    }

    public boolean setPlate(String fileId, String plateNumber) {
        return backend.setHomelessNumber(fileId,plateNumber);
    }

    public Optional<CheckPlateResponse> getPlateById(String fileId) {
         return backend.checkFile(fileId);
    }
}
