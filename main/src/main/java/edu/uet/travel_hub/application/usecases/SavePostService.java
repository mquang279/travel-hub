package edu.uet.travel_hub.application.usecases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.application.dto.response.SavePostResponse;
import edu.uet.travel_hub.application.exception.ResourceNotFoundException;
import edu.uet.travel_hub.application.port.in.SavePostUseCase;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.port.out.PostRepository;
import edu.uet.travel_hub.application.port.out.SavedPostRepository;

@Service
public class SavePostService implements SavePostUseCase {
    private final SavedPostRepository savedPostRepository;
    private final PostRepository postRepository;
    private final CurrentUserProvider currentUserProvider;

    public SavePostService(SavedPostRepository savedPostRepository, PostRepository postRepository,
            CurrentUserProvider currentUserProvider) {
        this.savedPostRepository = savedPostRepository;
        this.postRepository = postRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    @Transactional
    public SavePostResponse save(Long postId) {
        Long userId = this.currentUserProvider.getCurrentUserId();
        ensurePostExists(postId);

        if (this.savedPostRepository.exists(userId, postId)) {
            return new SavePostResponse(
                    postId,
                    true,
                    Math.toIntExact(this.savedPostRepository.countByPostId(postId)));
        }

        this.savedPostRepository.save(userId, postId);
        return new SavePostResponse(
                postId,
                true,
                Math.toIntExact(this.savedPostRepository.countByPostId(postId)));
    }

    @Override
    @Transactional
    public SavePostResponse unsave(Long postId) {
        Long userId = this.currentUserProvider.getCurrentUserId();
        ensurePostExists(postId);

        this.savedPostRepository.delete(userId, postId);

        return new SavePostResponse(
                postId,
                false,
                Math.toIntExact(this.savedPostRepository.countByPostId(postId)));
    }

    private void ensurePostExists(Long postId) {
        this.postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
    }
}
