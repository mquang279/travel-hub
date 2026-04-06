package edu.uet.travel_hub.application.usecases;

import org.springframework.stereotype.Service;

import edu.uet.travel_hub.application.dto.response.PaginationResponse;
import edu.uet.travel_hub.application.port.in.GetAllPostsUseCase;
import edu.uet.travel_hub.application.port.out.PostRepository;
import edu.uet.travel_hub.domain.model.PostModel;

@Service
public class GetAllPostsService implements GetAllPostsUseCase {
    private final PostRepository postRepository;

    public GetAllPostsService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    public PaginationResponse<PostModel> getAll(int pageNumber, int pageSize) {
        return this.postRepository.getAll(pageNumber, pageSize);
    }

}
