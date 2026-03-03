package ru.parking.parking_bot.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CleaningPlanResponse {
    LocalDate cleaningDate;
    Integer startParkingNumber;
    Integer endParkingNumber;
}
