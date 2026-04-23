package edu.uet.travel_hub.application.port.out;

import java.util.List;

import edu.uet.travel_hub.domain.model.PostEmbeddingSyncModel;
import edu.uet.travel_hub.domain.model.TravelPlaceRecommendationModel;
import edu.uet.travel_hub.domain.model.TravelPlaceRecommendationQuery;


public interface AiEmbeddingGateway {
    void upsertPostEmbedding(PostEmbeddingSyncModel model);

    List<TravelPlaceRecommendationModel> recommendTravelPlaces(TravelPlaceRecommendationQuery query);
}
