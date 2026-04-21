package edu.uet.travel_hub.domain.model;

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

    private int likeCount;

    private Long userId;
}
