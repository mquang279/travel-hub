package edu.uet.travel_hub.application.port.in;

public interface DeleteCommentUseCase {
    void delete(Long postId, Long commentId);
}