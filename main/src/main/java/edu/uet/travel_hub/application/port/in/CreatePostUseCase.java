package edu.uet.travel_hub.application.port.in;

import edu.uet.travel_hub.application.dto.request.CreatePostRequest;
import edu.uet.travel_hub.domain.model.PostModel;

public interface CreatePostUseCase {
    PostModel create(CreatePostRequest request);
}
