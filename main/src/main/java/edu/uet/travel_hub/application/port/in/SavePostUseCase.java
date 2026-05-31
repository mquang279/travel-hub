package edu.uet.travel_hub.application.port.in;

import edu.uet.travel_hub.application.dto.response.SavePostResponse;

public interface SavePostUseCase {
    SavePostResponse save(Long postId);

    SavePostResponse unsave(Long postId);
}
