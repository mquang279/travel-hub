package edu.uet.travel_hub.application.port.out;

import edu.uet.travel_hub.application.dto.response.PaginationResponse;
import edu.uet.travel_hub.domain.model.CommentModel;

public interface CommentRepository {
    PaginationResponse<CommentModel> findByPostId(int page, int pageSize, Long postId);

    CommentModel findById(Long commentId);

    CommentModel save(CommentModel comment);

    void delete(Long commentId);
}
