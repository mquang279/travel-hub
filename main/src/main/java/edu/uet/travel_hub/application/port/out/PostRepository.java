package edu.uet.travel_hub.application.port.out;

import java.util.Optional;

import edu.uet.travel_hub.domain.model.PostModel;

public interface PostRepository {
    PostModel save(Long userId, PostModel post);

    Optional<PostModel> findById(Long id);
}
