package edu.uet.travel_hub.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import edu.uet.travel_hub.domain.model.PostModel;
import edu.uet.travel_hub.infrastructure.persistence.entity.PostEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.TravelPlaceEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TravelPlaceJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.UserJpaRepository;

@Component
public class PostPersistenceMapper {
    private final UserJpaRepository userJpaRepository;
    private final TravelPlaceJpaRepository travelPlaceJpaRepository;

    public PostPersistenceMapper(UserJpaRepository userJpaRepository, TravelPlaceJpaRepository travelPlaceJpaRepository) {
        this.userJpaRepository = userJpaRepository;
        this.travelPlaceJpaRepository = travelPlaceJpaRepository;
    }

    public PostEntity toEntity(PostModel model) {
        if (model.getUserId() == null) {
            throw new IllegalArgumentException("Post userId must not be null");
        }

        UserEntity user = this.userJpaRepository.findById(model.getUserId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found with id: " + model.getUserId()));

        TravelPlaceEntity travelPlace = null;
        if (model.getTravelPlaceId() != null) {
            travelPlace = this.travelPlaceJpaRepository.findById(model.getTravelPlaceId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Travel place not found with id: " + model.getTravelPlaceId()));
        }

        return PostEntity.builder()
                .id(model.getId())
                .description(model.getDescription())
                .imageUrls(model.getImageUrls())
                .travelPlace(travelPlace)
                .likeCount(model.getLikeCount())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .commentCount(model.getCommentCount())
                .user(user)
                .build();
    }

    public PostModel toDomain(PostEntity entity) {
        String location = null;
        Long travelPlaceId = null;
        if (entity.getTravelPlace() != null) {
            travelPlaceId = entity.getTravelPlace().getId();
            String provinceName = entity.getTravelPlace().getProvince() != null
                    ? entity.getTravelPlace().getProvince().getName()
                    : null;
            location = provinceName == null || provinceName.isBlank()
                    ? entity.getTravelPlace().getName()
                    : entity.getTravelPlace().getName() + ", " + provinceName;
        }

        return PostModel.builder()
                .description(entity.getDescription())
                .imageUrls(entity.getImageUrls())
                .id(entity.getId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .commentCount(entity.getCommentCount())
                .likeCount(entity.getLikeCount())
                .travelPlaceId(travelPlaceId)
                .location(location)
                .userId(entity.getUser().getId())
                .ownerUsername(entity.getUser().getUsername())
                .build();
    }
}
