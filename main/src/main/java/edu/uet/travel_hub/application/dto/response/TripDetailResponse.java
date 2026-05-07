package edu.uet.travel_hub.application.dto.response;

import java.util.List;

public record TripDetailResponse(
        TripInfoResponse tripInfo,
        String myRole,
        TripMembersResponse members,
        List<TripHighlightResponse> highlights,
        List<TripActivityLogResponse> recentActivities) {
}