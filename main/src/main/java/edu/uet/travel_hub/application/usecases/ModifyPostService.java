package edu.uet.travel_hub.application.usecases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.application.dto.request.ModifyPostRequest;
import edu.uet.travel_hub.application.port.in.ModifyPostUseCase;
import edu.uet.travel_hub.application.port.out.AiEmbeddingGateway;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.port.out.PostRepository;
import edu.uet.travel_hub.domain.model.PostEmbeddingSyncModel;
import edu.uet.travel_hub.domain.model.PostModel;
import edu.uet.travel_hub.infrastructure.persistence.entity.TravelPlaceEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TravelPlaceJpaRepository;

@Service
public class ModifyPostService implements ModifyPostUseCase {
    private final PostRepository postRepository;
    private final CurrentUserProvider userProvider;
    private final TravelPlaceJpaRepository travelPlaceJpaRepository;
    private final AiEmbeddingGateway aiEmbeddingGateway;

    public ModifyPostService(
            PostRepository postRepository,
            CurrentUserProvider userProvider,
            TravelPlaceJpaRepository travelPlaceJpaRepository,
            AiEmbeddingGateway aiEmbeddingGateway) {
        this.postRepository = postRepository;
        this.userProvider = userProvider;
        this.travelPlaceJpaRepository = travelPlaceJpaRepository;
        this.aiEmbeddingGateway = aiEmbeddingGateway;
    }

    @Override
    @Transactional
    public PostModel modify(Long postId, ModifyPostRequest request) {
        Long userId = this.userProvider.getCurrentUserId();
        PostModel post = this.postRepository.findById(postId).get();
        if (request.description() != null) {
            post.setDescription(request.description());
        }
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
