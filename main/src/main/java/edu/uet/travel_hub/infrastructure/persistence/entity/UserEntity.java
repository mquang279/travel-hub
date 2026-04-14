package edu.uet.travel_hub.infrastructure.persistence.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import edu.uet.travel_hub.domain.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;

    private String name;

    @Column(unique = true)
    @Email
    private String email;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String hashPassword;

    private String bio;

    private Integer age;

    private String phoneNumber;
    private String avatarUrl;

    private LocalDate dateOfBirth;
    private String gender;
    private String location;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int followersCount;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int followingCount;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int postsCount;

    @Column(length = 1000, unique = true)
    private String refreshToken;

    private Instant createdAt;

    private Instant updatedAt;

    @OneToMany(mappedBy = "user")
    private List<PostJpaEntity> posts;

    @PrePersist
    public void handleBeforeCreate() {
        this.createdAt = Instant.now();
    }

    @PreUpdate
    public void handleBeforeUpdate() {
        this.updatedAt = Instant.now();
    }
}
