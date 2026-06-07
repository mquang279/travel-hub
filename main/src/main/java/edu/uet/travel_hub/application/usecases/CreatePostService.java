package edu.uet.travel_hub.application.usecases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.application.dto.request.CreatePostRequest;
import edu.uet.travel_hub.application.port.in.CreatePostUseCase;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.port.out.PostRepository;
import edu.uet.travel_hub.application.port.out.UserRepository;
import edu.uet.travel_hub.domain.model.PostModel;

@Service
public class CreatePostService implements CreatePostUseCase {
    private final CurrentUserProvider userProvider;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public CreatePostService(
            CurrentUserProvider userProvider,
            PostRepository postRepository,
            UserRepository userRepository) {
        this.userProvider = userProvider;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public PostModel create(CreatePostRequest request) {
        Long userId = userProvider.getCurrentUserId();

        PostModel post = PostModel.builder()
                .description(request.description())
                .imageUrls(request.imageUrls())
                .travelPlaceId(request.travelPlaceId())
                .userId(userId)
                .build();
        PostModel savedPost = this.postRepository.save(userId, post);
        this.userRepository.incrementPosts(userId);
        return savedPost;
    }
}
