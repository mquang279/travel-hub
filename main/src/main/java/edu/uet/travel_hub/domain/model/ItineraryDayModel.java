package edu.uet.travel_hub.domain.model;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItineraryDayModel {
    private Long id;
    private int dayIndex;
    private String label;
    private String dateLabel;

    @Builder.Default
    private List<ItineraryStopModel> stops = new ArrayList<>();
}
