package edu.uet.travel_hub.application.port.in;

import edu.uet.travel_hub.application.dto.response.PaginationResponse;
import edu.uet.travel_hub.domain.model.PostModel;

public interface GetPostsOfUserUseCase {
    PaginationResponse<PostModel> get(Long userId, int pageNumber, int pageSize);
}
