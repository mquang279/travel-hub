package edu.uet.travel_hub.domain.enums;

import java.time.LocalDate;

public enum TripStatus {
    PLANNING,
    UPCOMING,
    ONGOING,
    COMPLETED;

    public static TripStatus fromDates(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        if (startDate == null || endDate == null) {
            return PLANNING;
        }
        if (endDate.isBefore(today)) {
            return COMPLETED;
        }
        if (!startDate.isAfter(today) && !endDate.isBefore(today)) {
            return ONGOING;
        }
        return UPCOMING;
    }
}