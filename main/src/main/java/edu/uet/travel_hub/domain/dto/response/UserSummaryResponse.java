package edu.uet.travel_hub.domain.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserSummaryResponse {
    private Long id;
    private String avatarUrl;
    private String name;
    private String username;
    private boolean isFollowing; 
}
