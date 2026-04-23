package edu.uet.travel_hub.infrastructure.client;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import edu.uet.travel_hub.application.port.out.AiEmbeddingGateway;
import edu.uet.travel_hub.domain.model.PostEmbeddingSyncModel;
import edu.uet.travel_hub.domain.model.TravelPlaceRecommendationModel;
import edu.uet.travel_hub.domain.model.TravelPlaceRecommendationQuery;


@Component
public class AiEmbeddingHttpGateway implements AiEmbeddingGateway {
    private final WebClient aiWebClient;

    public AiEmbeddingHttpGateway(WebClient aiWebClient) {
        this.aiWebClient = aiWebClient;
    }

    @Override
    public void upsertPostEmbedding(PostEmbeddingSyncModel model) {
        this.aiWebClient.put()
                .uri("/api/posts/{postId}/embedding", model.postId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PostEmbeddingPayload(
                        model.description(),
                        model.imageUrls(),
                        model.travelPlaceId(),
                        model.travelPlaceName(),
                        model.travelPlaceDescription(),
                        model.provinceName(),
                        model.openingTime(),
                        model.lat(),
                        model.lon()))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    @Override
    public List<TravelPlaceRecommendationModel> recommendTravelPlaces(TravelPlaceRecommendationQuery query) {
        RecommendationResponse response = this.aiWebClient.post()
                .uri("/api/users/{userId}/travel-place-recommendations", query.userId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RecommendationRequest(
                        query.viewedPlaceIds(),
                        query.provinceId(),
                        query.limit(),
                        query.offset()))
                .retrieve()
                .bodyToMono(RecommendationResponse.class)
                .block();

        if (response == null || response.items() == null) {
            return List.of();
        }

        return response.items().stream()
                .map(item -> TravelPlaceRecommendationModel.builder()
                        .travelPlaceId(item.travelPlaceId())
                        .score(item.score())
                        .preferenceScore(item.preferenceScore())
                        .historyScore(item.historyScore())
                        .popularityScore(item.popularityScore())
                        .build())
                .toList();
    }

    private record PostEmbeddingPayload(
            String description,
            List<String> imageUrls,
            Long travelPlaceId,
            String travelPlaceName,
            String travelPlaceDescription,
            String provinceName,
            String openingTime,
            Double lat,
            Double lon) {
    }

    private record RecommendationRequest(
            List<Long> viewedPlaceIds,
            Long provinceId,
            int limit,
            int offset) {
    }

    private record RecommendationResponse(
            Long userId,
            List<RecommendationItem> items) {
    }

    private record RecommendationItem(
            Long travelPlaceId,
            double score,
            double preferenceScore,
            double historyScore,
            double popularityScore) {
    }
}
