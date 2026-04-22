package edu.uet.travel_hub.application.usecases;

import org.springframework.stereotype.Service;

import edu.uet.travel_hub.application.dto.response.LikePostResponse;
import edu.uet.travel_hub.application.port.in.LikePostUseCase;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.port.out.LikeRepository;
import edu.uet.travel_hub.application.port.out.PostRepository;

@Service
public class LikePostService implements LikePostUseCase {
    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final CurrentUserProvider currentUserProvider;

    public LikePostService(LikeRepository likeRepository, CurrentUserProvider currentUserProvider,
            PostRepository postRepository) {
        this.likeRepository = likeRepository;
        this.currentUserProvider = currentUserProvider;
        this.postRepository = postRepository;
    }

    @Override
    public LikePostResponse like(Long postId) {
        Long userId = this.currentUserProvider.getCurrentUserId();
        this.likeRepository.save(userId, postId);
        this.postRepository.increaseLikeCount(postId);
        int likeCount = this.postRepository.getLikeCount(postId);
        return new LikePostResponse(postId, true, likeCount);
    }

}
