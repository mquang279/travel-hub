package edu.uet.travel_hub.application.usecases;

import org.springframework.stereotype.Service;

import edu.uet.travel_hub.application.dto.response.PaginationResponse;
import edu.uet.travel_hub.application.port.in.GetAllPostsUseCase;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.port.out.LikeRepository;
import edu.uet.travel_hub.application.port.out.PostRepository;
import edu.uet.travel_hub.domain.model.PostModel;

@Service
public class GetAllPostsService implements GetAllPostsUseCase {
    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final CurrentUserProvider currentUserProvider;

    public GetAllPostsService(PostRepository postRepository, LikeRepository likeRepository,
            CurrentUserProvider currentUserProvider) {
        this.postRepository = postRepository;
        this.likeRepository = likeRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public PaginationResponse<PostModel> getAll(int pageNumber, int pageSize) {
        PaginationResponse<PostModel> posts = this.postRepository.getAll(pageNumber, pageSize);
        Long currentUserId = getCurrentUserIdOrNull();

        for (PostModel post : posts.data()) {
            boolean isLiked = currentUserId != null && this.likeRepository.exists(currentUserId, post.getId());
            post.setLiked(isLiked);
        }

        return posts;
    }

    private Long getCurrentUserIdOrNull() {
        try {
            return this.currentUserProvider.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }

}
