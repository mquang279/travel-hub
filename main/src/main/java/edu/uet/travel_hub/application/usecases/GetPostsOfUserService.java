package edu.uet.travel_hub.application.usecases;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import edu.uet.travel_hub.application.dto.response.PaginationResponse;
import edu.uet.travel_hub.application.exception.ResourceNotFoundException;
import edu.uet.travel_hub.application.port.in.GetPostsOfUserUseCase;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.port.out.LikeRepository;
import edu.uet.travel_hub.application.port.out.PostRepository;
import edu.uet.travel_hub.application.port.out.UserRepository;
import edu.uet.travel_hub.domain.model.PostModel;

@Service
public class GetPostsOfUserService implements GetPostsOfUserUseCase {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final CurrentUserProvider currentUserProvider;

    public GetPostsOfUserService(PostRepository postRepository, UserRepository userRepository,
            LikeRepository likeRepository, CurrentUserProvider currentUserProvider) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.likeRepository = likeRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public PaginationResponse<PostModel> get(Long userId, int pageNumber, int pageSize) {
        this.userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        PaginationResponse<PostModel> posts = this.postRepository.getByUserId(userId, pageNumber, pageSize);
        Long currentUserId = this.currentUserProvider.getOptionalCurrentUserId().orElse(null);
        Set<Long> likedPostIds = findLikedPostIds(currentUserId, posts.data());

        for (PostModel post : posts.data()) {
            post.setLiked(likedPostIds.contains(post.getId()));
        }

        return posts;
    }

    private Set<Long> findLikedPostIds(Long currentUserId, List<PostModel> posts) {
        if (currentUserId == null || posts.isEmpty()) {
            return Set.of();
        }

        List<Long> postIds = posts.stream()
                .map(PostModel::getId)
                .toList();
        return this.likeRepository.findLikedPostIds(currentUserId, postIds);
    }
}
