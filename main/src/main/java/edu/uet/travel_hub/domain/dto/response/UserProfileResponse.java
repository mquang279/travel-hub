package edu.uet.travel_hub.domain.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class UserProfileResponse {
    private Long id;
    private String avatarUrl;
    private String name;
    private String username;
    private String bio;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private String gender;
    private String location; // Country or City location
    private int followersCount;
    private int followingCount;
    private int postsCount;
    private boolean isFollowing; // Check if the current user is following this profile
}
