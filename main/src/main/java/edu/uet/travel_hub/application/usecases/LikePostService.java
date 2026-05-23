package edu.uet.travel_hub.application.usecases;

import org.springframework.stereotype.Service;

import edu.uet.travel_hub.application.dto.response.LikePostResponse;
import edu.uet.travel_hub.application.exception.ResourceNotFoundException;
import edu.uet.travel_hub.application.port.in.LikePostUseCase;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.port.out.LikeRepository;
import edu.uet.travel_hub.application.port.out.PostRepository;
import edu.uet.travel_hub.application.port.out.UserRepository;
import edu.uet.travel_hub.domain.enums.NotificationType;
import edu.uet.travel_hub.domain.model.PostModel;
import edu.uet.travel_hub.domain.model.UserModel;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class LikePostService implements LikePostUseCase {
    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final SaveNotificationService saveNotificationService;
    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;

    @Override
    public LikePostResponse like(Long postId) {
        Long userId = this.currentUserProvider.getCurrentUserId();
        PostModel postModel = this.postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!this.likeRepository.exists(userId, postId)) {
            UserModel likedByUser = this.userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            this.likeRepository.save(userId, postId);
            this.postRepository.increaseLikeCount(postId);

            String title = "New like on your post";
            String body = likedByUser.getUsername() + " liked your post";
            this.saveNotificationService.save(postModel.getUserId(), title, body, NotificationType.LIKE, postId);
        }

        int likeCount = this.postRepository.getLikeCount(postId);

        return new LikePostResponse(postId, true, likeCount);
    }
}
