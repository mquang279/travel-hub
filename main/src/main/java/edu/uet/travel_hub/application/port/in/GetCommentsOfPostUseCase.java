package edu.uet.travel_hub.application.port.in;

import edu.uet.travel_hub.application.dto.response.PaginationResponse;
import edu.uet.travel_hub.domain.model.CommentModel;

public interface GetCommentsOfPostUseCase {
    public PaginationResponse<CommentModel> get(int page, int pageSize, Long postId);
}
