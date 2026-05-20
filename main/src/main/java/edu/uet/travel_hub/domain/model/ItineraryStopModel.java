package edu.uet.travel_hub.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItineraryStopModel {
    private Long id;
    private int sortOrder;
    private String startTime;
    private String endTime;
    private String title;
    private String placeName;
    private String note;
    private String transportToNext;
    private String estimatedCost;
    private Long colorHex;
    private String iconName;
}
