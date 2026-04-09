package edu.uet.travel_hub.domain.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserFollowResponse {
    private Long id;
    private String name;
    private String username;
    private String avatarUrl;
    private boolean following;
}
