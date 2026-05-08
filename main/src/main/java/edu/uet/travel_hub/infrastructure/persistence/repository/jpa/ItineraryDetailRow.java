package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import java.time.Instant;

public interface ItineraryDetailRow {
    Long getItineraryId();

    String getGroupName();

    int getVersion();

    Long getOwnerId();

    Instant getCreatedAt();

    Instant getUpdatedAt();

    Long getDayId();

    Integer getDayIndex();

    String getDayLabel();

    String getDateLabel();

    Long getStopId();

    Integer getSortOrder();

    String getStartTime();

    String getEndTime();

    String getTitle();

    String getPlaceName();

    String getNote();

    String getTransportToNext();

    String getEstimatedCost();

    Long getColorHex();

    String getIconName();
}
