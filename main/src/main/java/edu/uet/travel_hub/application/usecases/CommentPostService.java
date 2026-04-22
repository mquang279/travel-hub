package edu.uet.travel_hub.application.usecases;

import org.springframework.stereotype.Service;

import edu.uet.travel_hub.application.dto.request.CommentRequest;
import edu.uet.travel_hub.application.port.in.CommentPostUseCase;
import edu.uet.travel_hub.application.port.out.CommentRepository;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.port.out.PostRepository;
import edu.uet.travel_hub.application.port.out.UserRepository;
import edu.uet.travel_hub.domain.model.CommentModel;

@Service
public class CommentPostService implements CommentPostUseCase {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CurrentUserProvider currentUserProvider;

    public CommentPostService(CommentRepository commentRepository, UserRepository userRepository,
            PostRepository postRepository, CurrentUserProvider currentUserProvider) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public CommentModel comment(Long postId, CommentRequest request) {
        Long userId = this.currentUserProvider.getCurrentUserId();
        CommentModel commentModel = CommentModel
                .builder()
                .content(request.content())
                .owner(this.userRepository.findById(userId).get())
                .post(this.postRepository.findById(postId).get()).build();
        return this.commentRepository.save(commentModel);
    }

}
