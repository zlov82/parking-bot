package ru.parking.parking_bot.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.parking.parking_bot.dto.CheckPlateResponse;
import ru.parking.parking_bot.dto.CleaningPlanResponse;
import ru.parking.parking_bot.dto.TImage;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class BackendApiClient {

    private final WebClient webClient;

    public BackendApiClient(
            @Qualifier("backendClient")
            WebClient webClient
    ) {
        this.webClient = webClient;
    }

    // Связать пользователя + парковочное место
    public boolean bindParkingNumberToUser(Long telegramId, String username, Integer parkingNumber) {
        CreateUserRequest request = new CreateUserRequest(telegramId, username, parkingNumber);
        try {
            HttpStatusCode statusCode = webClient.post()
                    .uri("/api/user")
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .map(response -> response.getStatusCode()) // теперь HttpStatusCode
                    .block();

            return statusCode != null && statusCode.is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Response POST /api/user {}", e.getMessage());
            return false;
        }
    }

    // Получить парковочные места пользователя
    public List<Integer> getParkingByUser(Long telegramId) {
        try {
            var responseEntity = webClient.get()
                    .uri("/api/user/{telegramId}", telegramId)
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<List<Integer>>() {
                    })
                    .block();

            if (responseEntity == null) {
                return Collections.emptyList();
            }

            log.info("response entity {}", responseEntity);
            HttpStatusCode status = responseEntity.getStatusCode();

            if (status.is2xxSuccessful()) {
                List<Integer> body = responseEntity.getBody();
                return body != null ? body : Collections.emptyList();
            } else {
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.warn(e.getMessage());
            return Collections.emptyList();
        }
    }

    //Отвязать парковочное место пользователя
    public boolean deleteUserParking(Long userId, Integer parkingId) {
        try {
            HttpStatusCode statusCode = webClient.delete()
                    .uri("/api/user/{userId}/{parkingId}", userId, parkingId)
                    .retrieve()
                    .toBodilessEntity()
                    .map(ResponseEntity::getStatusCode)
                    .block();

            return statusCode != null && statusCode.is2xxSuccessful();
        } catch (Exception e) {
            log.warn(e.getMessage());
            return false;
        }
    }

    // Даты уборки машиномест, закрепленных за пользотвателем
    public Optional<Map<String, List<LocalDate>>> getUserCleanings(Long telegramId) {
        return webClient.get()
                .uri("/api/v1/cleaning/user/{telegramId}", telegramId)
                .exchangeToMono(response -> {
                    if (response.statusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.empty(); // нормальный кейс
                    }
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(
                                new ParameterizedTypeReference<Map<String, List<LocalDate>>>() {
                                }
                        );
                    }
                    return response.createException().flatMap(Mono::error);
                })
                .blockOptional();
    }

    //Создаем расписание
    public boolean createScheduleCleaning(String cleaningDate, Integer startPark, Integer endPark) {
        CleaningRequest request = new CleaningRequest(cleaningDate, startPark, endPark);
        log.info("Request {}", request);
        try {
            HttpStatusCode statusCode = webClient.post()
                    .uri("/api/v1/cleaning")
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .map(response -> response.getStatusCode()) // теперь HttpStatusCode
                    .block();

            return statusCode != null && statusCode.is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Error response POST /api/v1/cleaning\n {}", e.getMessage());
            return false;
        }
    }

    public List<CleaningPlanResponse> getNextCleaningPlans(int count) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/cleaning/next")
                        .queryParam("limit", count)
                        .build())
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(
                                new ParameterizedTypeReference<List<CleaningPlanResponse>>() {
                                }
                        ).defaultIfEmpty(Collections.emptyList());
                    }
                    return Mono.just(Collections.emptyList());
                })
                .onErrorResume(e -> {
                    return Mono.just(Collections.emptyList());
                })
                .block();
    }

    public boolean deleteCleaningPlan(LocalDate date) {
        try {
            HttpStatusCode statusCode = webClient.delete()
                    .uri("/api/v1/cleaning/{dateCleaning}", date)
                    .retrieve()
                    .toBodilessEntity()
                    .map(ResponseEntity::getStatusCode) // теперь HttpStatusCode
                    .block();

            return statusCode != null && statusCode.is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Problems with removal cleaning date {}", e.getMessage());
            return false;
        }
    }

    public Optional<CleaningPlanResponse> getCleaningPlan(LocalDate date) {
        return webClient.get()
                .uri("/api/v1/cleaning/date/{cleaningDate}", date)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(
                                        new ParameterizedTypeReference<List<CleaningPlanResponse>>() {
                                        }
                                ).map(list -> list.stream().findFirst())
                                .defaultIfEmpty(Optional.empty());
                    }
                    return Mono.just(Optional.empty());
                })
                .onErrorResume(e -> {
                    return Mono.just(Optional.empty());
                })
                .block();
    }

    public List<LocalDate> getFreeCleanings() {
        return webClient.get()
                .uri("/api/v1/cleaning/free/next")
                .exchangeToMono(clientResponse -> {
                    if (clientResponse.statusCode().is2xxSuccessful()) {
                        return clientResponse.bodyToMono(
                                new ParameterizedTypeReference<List<LocalDate>>() {
                                }
                        ).defaultIfEmpty(Collections.emptyList());
                    }
                    return Mono.just(Collections.emptyList());
                })
                .onErrorResume(e -> {
                    return Mono.just(Collections.emptyList());
                })
                .block();
    }

    public boolean createFreeCleaning(LocalDate date) {
        try {
            HttpStatusCode statusCode = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/cleaning/free")
                            .queryParam("date", date)
                            .build())
                    .retrieve()
                    .toBodilessEntity()
                    .map(ResponseEntity::getStatusCode)
                    .block();
            log.debug("Response status {} on POST /api/v1/cleaning/free", statusCode);
            return statusCode != null && statusCode.is2xxSuccessful();
        } catch (Exception e) {
            log.warn(e.getMessage());
            return false;
        }
    }

    public boolean deleteFreeCleaning(LocalDate date) {
        try {
            HttpStatusCode statusCode = webClient.delete()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/cleaning/free")
                            .queryParam("date", date)
                            .build())
                    .retrieve()
                    .toBodilessEntity()
                    .map(ResponseEntity::getStatusCode)
                    .block();
            log.debug("Response status {} on DELETE /api/v1/cleaning/free", statusCode);
            return statusCode != null && statusCode.is2xxSuccessful();
        } catch (Exception e) {
            log.warn(e.getMessage());
            return false;
        }
    }

    public List<String> uploadTImage(TImage image) {

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        ByteArrayResource resource = new ByteArrayResource(image.getData()) {
            @Override
            public String getFilename() {
                return image.getFilePath() != null ? image.getFilePath() : "photo.jpg";
            }
        };

        body.add("file", resource);
        body.add("telegramFileId", image.getFileId());
        body.add("userId", image.getUserId().toString());
        body.add("chatId", image.getChatId().toString());

        try {
            List<String> plates = webClient.post()
                    .uri("/homeless/upload")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(body))
                    .exchangeToMono(response -> {
                        int status = response.statusCode().value();
                        log.info("HTTP status {}", status);
                        return response.bodyToMono(
                                new ParameterizedTypeReference<List<String>>() {
                                }
                        ).doOnNext(p -> log.info("Response body {}", p));
                    })
                    .block();

            return plates != null ? plates : List.of();
        } catch (Exception e) {
            log.error("Upload failed {}", e.getMessage());
            return List.of();
        }
    }

    public boolean setHomelessNumber(String fileId, String plate) {
        try {
            HttpStatusCode statusCode = webClient.post()
                    .uri("/homeless/{fileId}/{plateNumber}", fileId, plate)
                    .retrieve()
                    .toBodilessEntity()
                    .map(ResponseEntity::getStatusCode)
                    .block();
            log.debug("Response status {} on POST /homeless/{fileId}/{plateNumber}", statusCode);
            return statusCode != null && statusCode.is2xxSuccessful();
        } catch (Exception e) {
            log.warn(e.getMessage());
            return false;
        }
    }

    public Optional<CheckPlateResponse> checkFile(String fileId) {
        return webClient.get()
                .uri("/homeless/{fileId}", fileId)
                .exchangeToMono(response -> {
                    if (response.statusCode() == HttpStatus.NOT_FOUND) {
                        return Mono.empty();
                    }
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(
                                new ParameterizedTypeReference<CheckPlateResponse>() {
                                }
                        );
                    }
                    return response.createException().flatMap(Mono::error);
                })
                .blockOptional();
    }

    // DTO
    private record CleaningRequest(
            String cleaningDate,
            Integer startParkingNumber,
            Integer endParkingNumber
    ) {
    }


    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private record CreateUserRequest(
            Long telegramId,
            String username,
            Integer parkingNumber
    ) {
    }

    @lombok.Data
    public static class ShortCleaningResponse {
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
        private java.time.LocalDate cleaningDate;

        @jakarta.validation.constraints.Positive
        private Integer parkingNumber;
    }


}
