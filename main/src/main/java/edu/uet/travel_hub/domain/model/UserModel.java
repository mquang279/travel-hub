package edu.uet.travel_hub.domain.model;

import java.time.LocalDate;

import edu.uet.travel_hub.domain.enums.Role;
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
    private int followersCount;
    private int followingCount;
    private int postsCount;
}