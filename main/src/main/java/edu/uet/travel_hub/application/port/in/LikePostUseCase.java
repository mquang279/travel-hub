package edu.uet.travel_hub.application.port.in;

import edu.uet.travel_hub.application.dto.response.LikePostResponse;

public interface LikePostUseCase {
    LikePostResponse like(Long postId);
}
