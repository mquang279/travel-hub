package edu.uet.travel_hub.application.dto.response;

import java.util.List;

public record TravelAssistantChatResponse(
        String answer,
        List<TravelAssistantPlaceReferenceResponse> places) {
}
