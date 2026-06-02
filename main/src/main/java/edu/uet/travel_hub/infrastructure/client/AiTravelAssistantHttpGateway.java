package edu.uet.travel_hub.infrastructure.client;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.annotation.JsonProperty;

import edu.uet.travel_hub.application.dto.request.TravelAssistantChatRequest;
import edu.uet.travel_hub.application.dto.response.TravelAssistantChatResponse;
import edu.uet.travel_hub.application.dto.response.TravelAssistantPlaceReferenceResponse;

@Component
public class AiTravelAssistantHttpGateway {
    private final WebClient aiWebClient;

    public AiTravelAssistantHttpGateway(WebClient aiWebClient) {
        this.aiWebClient = aiWebClient;
    }

    public TravelAssistantChatResponse chat(TravelAssistantChatRequest request) {
        AiTravelAssistantResponse response = this.aiWebClient.post()
                .uri("/api/travel-assistant/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiTravelAssistantResponse.class)
                .block();

        if (response == null) {
            return new TravelAssistantChatResponse("", List.of());
        }

        List<TravelAssistantPlaceReferenceResponse> places = response.places() == null
                ? List.of()
                : response.places().stream()
                        .map(place -> new TravelAssistantPlaceReferenceResponse(
                                place.id(),
                                place.name(),
                                place.province(),
                                place.averageRating(),
                                place.reviewCount()))
                        .toList();

        return new TravelAssistantChatResponse(response.answer(), places);
    }

    private record AiTravelAssistantResponse(
            String answer,
            List<AiTravelAssistantPlaceReference> places) {
    }

    private record AiTravelAssistantPlaceReference(
            Long id,
            String name,
            String province,
            @JsonProperty("average_rating")
            Double averageRating,
            @JsonProperty("review_count")
            long reviewCount) {
    }
}
