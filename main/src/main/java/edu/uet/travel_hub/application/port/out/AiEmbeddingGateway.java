package edu.uet.travel_hub.application.port.out;

import edu.uet.travel_hub.domain.model.PostEmbeddingSyncModel;


public interface AiEmbeddingGateway {
    void upsertPostEmbedding(PostEmbeddingSyncModel model);
}
