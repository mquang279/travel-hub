package edu.uet.travel_hub.application.usecases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.application.dto.request.CreatePostRequest;
import edu.uet.travel_hub.application.port.in.CreatePostUseCase;
import edu.uet.travel_hub.application.port.out.AiEmbeddingGateway;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.port.out.PostRepository;
import edu.uet.travel_hub.domain.model.PostEmbeddingSyncModel;
import edu.uet.travel_hub.domain.model.PostModel;
import edu.uet.travel_hub.infrastructure.persistence.entity.TravelPlaceEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TravelPlaceJpaRepository;

@Service
public class CreatePostService implements CreatePostUseCase {
    private final CurrentUserProvider userProvider;
    private final PostRepository postRepository;
    private final TravelPlaceJpaRepository travelPlaceJpaRepository;
    private final AiEmbeddingGateway aiEmbeddingGateway;

    public CreatePostService(
            CurrentUserProvider userProvider,
            PostRepository postRepository,
            TravelPlaceJpaRepository travelPlaceJpaRepository,
            AiEmbeddingGateway aiEmbeddingGateway) {
        this.userProvider = userProvider;
        this.postRepository = postRepository;
        this.travelPlaceJpaRepository = travelPlaceJpaRepository;
        this.aiEmbeddingGateway = aiEmbeddingGateway;
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
        this.aiEmbeddingGateway.upsertPostEmbedding(toEmbeddingModel(savedPost));
        return savedPost;
    }

    private PostEmbeddingSyncModel toEmbeddingModel(PostModel post) {
        TravelPlaceEntity place = resolveTravelPlace(post.getTravelPlaceId());
        return PostEmbeddingSyncModel.builder()
                .postId(post.getId())
                .description(post.getDescription())
                .imageUrls(post.getImageUrls())
                .travelPlaceId(post.getTravelPlaceId())
                .travelPlaceName(place == null ? null : place.getName())
                .travelPlaceDescription(place == null ? null : place.getDescription())
                .provinceName(place == null || place.getProvince() == null ? null : place.getProvince().getName())
                .openingTime(place == null ? null : place.getOpeningTime())
                .lat(place == null ? null : place.getLat())
                .lon(place == null ? null : place.getLon())
                .build();
    }

    private TravelPlaceEntity resolveTravelPlace(Long travelPlaceId) {
        if (travelPlaceId == null) {
            return null;
        }
        return this.travelPlaceJpaRepository.findById(travelPlaceId).orElse(null);
    }
}
