package edu.uet.travel_hub.application.usecases;

import org.springframework.stereotype.Service;

import edu.uet.travel_hub.application.dto.response.LikePostResponse;
import edu.uet.travel_hub.application.port.in.UnlikePostUseCase;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.port.out.LikeRepository;
import edu.uet.travel_hub.application.port.out.PostRepository;

@Service
public class UnlikePostService implements UnlikePostUseCase {
    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final CurrentUserProvider currentUserProvider;

    public UnlikePostService(LikeRepository likeRepository, PostRepository postRepository,
            CurrentUserProvider currentUserProvider) {
        this.likeRepository = likeRepository;
        this.postRepository = postRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public LikePostResponse unlike(Long postId) {
        Long userId = this.currentUserProvider.getCurrentUserId();
        this.likeRepository.delete(userId, postId);
        this.postRepository.decreaseLikeCount(postId);
        int likeCount = this.postRepository.getLikeCount(postId);
        return new LikePostResponse(postId, false, likeCount);
    }

}
