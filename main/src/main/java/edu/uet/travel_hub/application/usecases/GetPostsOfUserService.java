package edu.uet.travel_hub.application.usecases;

import org.springframework.stereotype.Service;

import edu.uet.travel_hub.application.dto.response.PaginationResponse;
import edu.uet.travel_hub.application.exception.ResourceNotFoundException;
import edu.uet.travel_hub.application.port.in.GetPostsOfUserUseCase;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.port.out.LikeRepository;
import edu.uet.travel_hub.application.port.out.PostRepository;
import edu.uet.travel_hub.application.port.out.UserRepository;
import edu.uet.travel_hub.domain.model.PostModel;

@Service
public class GetPostsOfUserService implements GetPostsOfUserUseCase {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final CurrentUserProvider currentUserProvider;

    public GetPostsOfUserService(PostRepository postRepository, UserRepository userRepository,
            LikeRepository likeRepository, CurrentUserProvider currentUserProvider) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.likeRepository = likeRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public PaginationResponse<PostModel> get(Long userId, int pageNumber, int pageSize) {
        this.userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        PaginationResponse<PostModel> posts = this.postRepository.getByUserId(userId, pageNumber, pageSize);
        Long currentUserId = this.currentUserProvider.getOptionalCurrentUserId().orElse(null);

        for (PostModel post : posts.data()) {
            boolean isLiked = currentUserId != null && this.likeRepository.exists(currentUserId, post.getId());
            post.setLiked(isLiked);
        }

        return posts;
    }
}
