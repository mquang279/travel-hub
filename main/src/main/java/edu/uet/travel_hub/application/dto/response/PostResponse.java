package edu.uet.travel_hub.application.dto.response;

import java.time.Instant;
import java.util.List;

import lombok.Builder;

@Builder
public record PostResponse(
                Long id,
                String description,
                List<String> imageUrls,
                UserResponse owner,
                int likeCount,
                int commentCount,
                int saveCount,
                boolean isLiked,
                boolean savedByCurrentUser,
                Long travelPlaceId,
                String location,
                Instant createdAt,
                Instant updatedAt) {
}
