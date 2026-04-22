package edu.uet.travel_hub.domain.model;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostModel {
    private Long id;

    private String description;

    private List<String> imageUrls;

    private String location;

    private Long travelPlaceId;

    private int likeCount;

    private int commentCount;

    private boolean isLiked;

    private Long userId;

    private Instant createdAt;

    private Instant updatedAt;
}
