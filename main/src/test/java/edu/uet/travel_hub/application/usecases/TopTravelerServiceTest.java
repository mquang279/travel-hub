package edu.uet.travel_hub.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import edu.uet.travel_hub.domain.enums.TopTravelerPeriod;

class TopTravelerServiceTest {
    private final TopTravelerService service = new TopTravelerService(null, null);

    @Test
    void weekStartsAtMondayMidnightInVietnam() {
        Instant now = Instant.parse("2026-05-24T05:00:00Z");

        assertEquals(
                Instant.parse("2026-05-17T17:00:00Z"),
                service.periodStart(TopTravelerPeriod.WEEK, now));
    }

    @Test
    void monthStartsAtFirstDayMidnightInVietnam() {
        Instant now = Instant.parse("2026-05-24T05:00:00Z");

        assertEquals(
                Instant.parse("2026-04-30T17:00:00Z"),
                service.periodStart(TopTravelerPeriod.MONTH, now));
    }
}
