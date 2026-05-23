package edu.uet.travel_hub.application.port.in;

import edu.uet.travel_hub.domain.model.PostModel;

public interface GetPostByIdUseCase {
    PostModel get(Long postId);
}
