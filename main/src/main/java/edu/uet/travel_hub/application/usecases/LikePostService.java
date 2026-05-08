package edu.uet.travel_hub.application.usecases;

import org.springframework.stereotype.Service;

import edu.uet.travel_hub.application.dto.response.LikePostResponse;
import edu.uet.travel_hub.application.exception.ResourceNotFoundException;
import edu.uet.travel_hub.application.port.in.LikePostUseCase;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.port.out.LikeRepository;
import edu.uet.travel_hub.application.port.out.PostRepository;
import edu.uet.travel_hub.domain.model.PostModel;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class LikePostService implements LikePostUseCase {
    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final SaveNotificationService saveNotificationService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public LikePostResponse like(Long postId) {
        Long userId = this.currentUserProvider.getCurrentUserId();
        PostModel postModel = this.postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!this.likeRepository.exists(userId, postId)) {
            this.likeRepository.save(userId, postId);
            this.postRepository.increaseLikeCount(postId);

            String title = "New notification";
            String body = "Your post received a new like";
            this.saveNotificationService.save(postModel.getUserId(), title, body);
        }

        int likeCount = this.postRepository.getLikeCount(postId);

        return new LikePostResponse(postId, true, likeCount);
    }

}
