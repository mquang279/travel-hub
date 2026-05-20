package edu.uet.travel_hub.domain.model;

import java.time.Instant;
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
public class ItineraryModel {
    private Long id;
    private String groupName;
    private int version;
    private Long ownerId;
    private Instant createdAt;
    private Instant updatedAt;

    @Builder.Default
    private List<ItineraryDayModel> days = new ArrayList<>();
}
