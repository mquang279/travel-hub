package edu.uet.travel_hub.application.dto.response;

import java.util.List;

public record TripMembersResponse(
        int totalMembers,
        int maxMembers,
        List<TripMemberResponse> members) {
}