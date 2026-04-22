package edu.uet.travel_hub.application.port.in;

import edu.uet.travel_hub.application.dto.request.CommentRequest;
import edu.uet.travel_hub.domain.model.CommentModel;

public interface CommentPostUseCase {
    CommentModel comment(Long postId, CommentRequest request);
}
