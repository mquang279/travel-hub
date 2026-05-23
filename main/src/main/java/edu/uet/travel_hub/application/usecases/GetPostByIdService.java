package edu.uet.travel_hub.application.usecases;

import org.springframework.stereotype.Service;

import edu.uet.travel_hub.application.exception.ResourceNotFoundException;
import edu.uet.travel_hub.application.port.in.GetPostByIdUseCase;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.port.out.LikeRepository;
import edu.uet.travel_hub.application.port.out.PostRepository;
import edu.uet.travel_hub.domain.model.PostModel;

@Service
public class GetPostByIdService implements GetPostByIdUseCase {
    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final CurrentUserProvider currentUserProvider;

    public GetPostByIdService(PostRepository postRepository, LikeRepository likeRepository,
            CurrentUserProvider currentUserProvider) {
        this.postRepository = postRepository;
        this.likeRepository = likeRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public PostModel get(Long postId) {
        PostModel post = this.postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        Long currentUserId = getCurrentUserIdOrNull();
        post.setLiked(currentUserId != null && this.likeRepository.exists(currentUserId, postId));
        return post;
    }

    private Long getCurrentUserIdOrNull() {
        try {
            return this.currentUserProvider.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
