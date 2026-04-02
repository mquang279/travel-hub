package edu.uet.travel_hub.application.usecases;

import edu.uet.travel_hub.application.dto.request.ModifyPostRequest;
import edu.uet.travel_hub.application.port.in.ModifyPostUseCase;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.port.out.PostRepository;
import edu.uet.travel_hub.domain.model.PostModel;

public class ModifyPostService implements ModifyPostUseCase {
    private final PostRepository postRepository;
    private final CurrentUserProvider userProvider;

    public ModifyPostService(PostRepository postRepository, CurrentUserProvider userProvider) {
        this.postRepository = postRepository;
        this.userProvider = userProvider;
    }

    @Override
    public PostModel modify(Long postId, ModifyPostRequest request) {
        Long userId = this.userProvider.getCurrentUserId();
        PostModel post = this.postRepository.findById(postId).get();
        if (request.description() != null) {
            post.setDescription(request.description());
        }
        return this.postRepository.save(userId, post);
    }
}
