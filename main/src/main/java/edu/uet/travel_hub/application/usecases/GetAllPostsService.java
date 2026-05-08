package edu.uet.travel_hub.application.usecases;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import edu.uet.travel_hub.application.dto.response.PaginationResponse;
import edu.uet.travel_hub.application.port.in.GetAllPostsUseCase;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.port.out.LikeRepository;
import edu.uet.travel_hub.application.port.out.PostRepository;
import edu.uet.travel_hub.domain.model.PostModel;

@Service
public class GetAllPostsService implements GetAllPostsUseCase {
    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final CurrentUserProvider currentUserProvider;

    public GetAllPostsService(PostRepository postRepository, LikeRepository likeRepository,
            CurrentUserProvider currentUserProvider) {
        this.postRepository = postRepository;
        this.likeRepository = likeRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public PaginationResponse<PostModel> getAll(int pageNumber, int pageSize) {
        PaginationResponse<PostModel> posts = this.postRepository.getAll(pageNumber, pageSize);
        Long currentUserId = getCurrentUserIdOrNull();
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

    private Long getCurrentUserIdOrNull() {
        try {
            return this.currentUserProvider.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }

}
