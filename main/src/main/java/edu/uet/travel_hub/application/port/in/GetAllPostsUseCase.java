package edu.uet.travel_hub.application.port.in;

import edu.uet.travel_hub.application.dto.response.PaginationResponse;
import edu.uet.travel_hub.domain.model.PostModel;

public interface GetAllPostsUseCase {
    public PaginationResponse<PostModel> getAll(int pageNumber, int pageSize);
}
