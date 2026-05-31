package edu.uet.travel_hub.application.usecases;

import org.springframework.stereotype.Service;

import edu.uet.travel_hub.application.dto.request.CommentRequest;
import edu.uet.travel_hub.application.port.in.CommentPostUseCase;
import edu.uet.travel_hub.application.port.out.CommentRepository;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.port.out.PostRepository;
import edu.uet.travel_hub.application.port.out.UserRepository;
import edu.uet.travel_hub.domain.enums.NotificationType;
import edu.uet.travel_hub.domain.model.CommentModel;
import edu.uet.travel_hub.domain.model.PostModel;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class CommentPostService implements CommentPostUseCase {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CurrentUserProvider currentUserProvider;
    private final SaveNotificationService saveNotificationService;

    @Override
    public CommentModel comment(Long postId, CommentRequest request) {
        Long userId = this.currentUserProvider.getCurrentUserId();
        PostModel postModel = this.postRepository.findById(postId).get();
        String commenterUsername = this.userRepository.findById(userId)
                .map(user -> user.getUsername())
                .orElse("Someone");
        CommentModel commentModel = CommentModel
                .builder()
                .content(request.content())
                .owner(this.userRepository.findById(userId).get())
                .post(this.postRepository.findById(postId).get()).build();
        if (!userId.equals(postModel.getUserId())) {
            String title = "New comment on your post";
            String body = commenterUsername + " commented on your post";
            this.saveNotificationService.save(postModel.getUserId(), title, body, NotificationType.COMMENT, postId);
        }
        return this.commentRepository.save(commentModel);
    }
}
