package ru.parking.parking_bot.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CleaningPlan {
    private LocalDate date;
    private Integer startParking;
    private Integer endParking;
}
