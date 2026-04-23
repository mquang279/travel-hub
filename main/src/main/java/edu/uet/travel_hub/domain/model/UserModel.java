package edu.uet.travel_hub.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class UserModel {
    private Long id;
    private String email;
    private String username;
    private String name;
    private String bio;
    private String phoneNumber;
    private String avatarUrl;
    private LocalDate dateOfBirth;
    private String gender;
    private String location;
    private String tripType;
    private List<String> interests;
    private String destination;
    private boolean isOnboarded;
    private int followersCount;
    private int followingCount;
    private int postsCount;
    private Instant createdAt;
    private Instant updatedAt;
}
