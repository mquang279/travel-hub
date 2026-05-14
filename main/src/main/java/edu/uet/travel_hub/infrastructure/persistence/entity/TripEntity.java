package edu.uet.travel_hub.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import edu.uet.travel_hub.domain.enums.TripStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "trips",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_trip_invite_code", columnNames = {"invite_code"})
        })
@Getter
@Setter
@ToString(exclude = {"leader", "members"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripEntity {
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 200)
    private String location;

    @Column(length = 1000)
    private String coverImageUrl;

    @Column(length = 2000)
    private String description;

    private LocalDate startDate;
    private LocalDate endDate;

    @Column(precision = 14, scale = 2)
    private BigDecimal budgetMin;

    @Column(precision = 14, scale = 2)
    private BigDecimal budgetMax;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TripStatus status;

    @Column(name = "invite_code", nullable = false, length = 100)
    private String inviteCode;
    
    @Column(name = "invite_code_expired_at")
    private LocalDateTime inviteCodeExpiredAt;
    @ManyToOne(optional = false)
    @JoinColumn(name = "leader_id", nullable = false)
    private UserEntity leader;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TripMemberEntity> members = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        if (this.status == null) {
            this.status = TripStatus.fromDates(this.startDate, this.endDate);
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
        this.status = TripStatus.fromDates(this.startDate, this.endDate);
    }

    // Backwards-compatible accessor used by some services
    public edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity getOwner() {
        return this.leader;
    }
}