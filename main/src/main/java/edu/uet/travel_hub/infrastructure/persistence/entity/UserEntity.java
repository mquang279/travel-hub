package edu.uet.travel_hub.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String username;
    private String bio;
    private String email;
    private String phoneNumber;
    private String avatarUrl;

    private LocalDate dateOfBirth;
    private String gender;
    private String location;

    private int followersCount;
    private int followingCount;
    private int postsCount;
}
