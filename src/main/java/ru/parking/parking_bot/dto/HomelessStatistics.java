package ru.parking.parking_bot.dto;

import lombok.Data;

import java.util.List;

@Data
public class HomelessStatistics {
    private Integer count;
    private Integer uniquePlates;
    private String period;
    private String periodText;
    private List<DailyUpload> dailyUploads;
    private List<TopPlate> topPlates;

    @Data
    public static class DailyUpload {
        private Integer count;
        private String date;
    }

    @Data
    public static class TopPlate {
        private Integer count;
        private String plateNumber;
    }
}
