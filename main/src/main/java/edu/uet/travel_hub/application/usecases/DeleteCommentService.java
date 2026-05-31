package edu.uet.travel_hub.application.usecases;

import org.springframework.stereotype.Service;

import edu.uet.travel_hub.application.port.in.DeleteCommentUseCase;
import edu.uet.travel_hub.application.port.out.CommentRepository;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.port.out.PostRepository;
import edu.uet.travel_hub.domain.model.CommentModel;

@Service
public class DeleteCommentService implements DeleteCommentUseCase {
    private final CommentRepository commentRepository;
    private final CurrentUserProvider currentUserProvider;
    private final PostRepository postRepository;

    public DeleteCommentService(
            CommentRepository commentRepository,
            CurrentUserProvider currentUserProvider,
            PostRepository postRepository) {
        this.commentRepository = commentRepository;
        this.currentUserProvider = currentUserProvider;
        this.postRepository = postRepository;
    }

    @Override
    public void delete(Long postId, Long commentId) {
        Long userId = this.currentUserProvider.getCurrentUserId();
        CommentModel commentModel = this.commentRepository.findById(commentId);
        if (!commentModel.getPost().getId().equals(postId)) {
            throw new RuntimeException("Error when deleting post.");
        } else if (!commentModel.getOwner().getId().equals(userId)) {
            throw new RuntimeException("You do not have permission to delete this comment.");
        }
        this.commentRepository.delete(commentId);
        this.postRepository.decreaseCommentCount(postId);
    }
}
