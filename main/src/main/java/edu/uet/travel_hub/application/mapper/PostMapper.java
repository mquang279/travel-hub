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
        UserModel userModel = this.userRepository.findById(model.getUserId()).get();
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
                .owner(toUserResponse(userModel)).build();
        return response;
    }

    private UserResponse toUserResponse(UserModel user) {
        return new UserResponse(
                user.getId(),
                user.getUsername());
    }
}
