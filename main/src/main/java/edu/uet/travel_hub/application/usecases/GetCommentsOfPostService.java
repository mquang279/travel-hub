package edu.uet.travel_hub.application.usecases;

import org.springframework.stereotype.Service;

import edu.uet.travel_hub.application.dto.response.PaginationResponse;
import edu.uet.travel_hub.application.port.in.GetCommentsOfPostUseCase;
import edu.uet.travel_hub.application.port.out.CommentRepository;
import edu.uet.travel_hub.domain.model.CommentModel;

@Service
public class GetCommentsOfPostService implements GetCommentsOfPostUseCase {
    private final CommentRepository commentRepository;

    public GetCommentsOfPostService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @Override
    public PaginationResponse<CommentModel> get(int page, int pageSize, Long postId) {
        return this.commentRepository.findByPostId(page, pageSize, postId);
    }

}
