package edu.uet.travel_hub.domain.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {
    private String name;
    private String username;
    private String bio;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private String avatarUrl;
    private String location;
}
