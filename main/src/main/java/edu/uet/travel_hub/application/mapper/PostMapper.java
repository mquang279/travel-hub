package edu.uet.travel_hub.application.mapper;

import org.springframework.stereotype.Component;

import edu.uet.travel_hub.application.dto.response.PostResponse;
import edu.uet.travel_hub.application.dto.response.UserResponse;
import edu.uet.travel_hub.application.port.out.UserRepository;
import edu.uet.travel_hub.domain.model.PostModel;
import edu.uet.travel_hub.domain.model.UserModel;

@Component
public class PostMapper {
    private final UserRepository userRepository;

    public PostMapper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public PostResponse toDto(PostModel model) {
        String ownerUsername = resolveOwnerUsername(model);
        PostResponse response = PostResponse.builder()
                .description(model.getDescription())
                .imageUrls(model.getImageUrls())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .id(model.getId())
                .likeCount(model.getLikeCount())
                .commentCount(model.getCommentCount())
                .isLiked(model.isLiked())
                .travelPlaceId(model.getTravelPlaceId())
                .location(model.getLocation())
                .owner(toUserResponse(model.getUserId(), ownerUsername)).build();
        return response;
    }

    private String resolveOwnerUsername(PostModel model) {
        if (model.getOwnerUsername() != null && !model.getOwnerUsername().isBlank()) {
            return model.getOwnerUsername();
        }
        if (model.getUserId() == null) {
            return null;
        }
        return this.userRepository.findById(model.getUserId())
                .map(UserModel::getUsername)
                .orElse(null);
    }

    private UserResponse toUserResponse(Long userId, String username) {
        return new UserResponse(
                userId,
                username);
    }
}
