package edu.uet.travel_hub.application.usecases;

import edu.uet.travel_hub.application.dto.request.CreatePostRequest;
import edu.uet.travel_hub.application.port.in.CreatePostUseCase;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.port.out.PostRepository;
import edu.uet.travel_hub.domain.model.PostModel;

public class CreatePostService implements CreatePostUseCase {
    private final CurrentUserProvider userProvider;
    private final PostRepository postRepository;

    public CreatePostService(CurrentUserProvider userProvider, PostRepository postRepository) {
        this.userProvider = userProvider;
        this.postRepository = postRepository;
    }

    @Override
    public PostModel create(CreatePostRequest request) {
        Long userId = userProvider.getCurrentUserId();
        PostModel post = PostModel.builder()
                .description(request.description())
                .imageUrl(request.imageUrl()).build();
        return this.postRepository.save(userId, post);
    }
}
