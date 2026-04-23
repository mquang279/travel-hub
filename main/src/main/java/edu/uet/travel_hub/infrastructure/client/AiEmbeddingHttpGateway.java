package edu.uet.travel_hub.infrastructure.client;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import edu.uet.travel_hub.application.port.out.AiEmbeddingGateway;
import edu.uet.travel_hub.domain.model.PostEmbeddingSyncModel;


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
}
