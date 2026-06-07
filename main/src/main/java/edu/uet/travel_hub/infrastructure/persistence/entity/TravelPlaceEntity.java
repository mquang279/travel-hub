package edu.uet.travel_hub.infrastructure.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "travel_places", indexes = {
        @jakarta.persistence.Index(name = "idx_travel_places_province_id", columnList = "province_id"),
        @jakarta.persistence.Index(name = "idx_travel_places_name", columnList = "name")
})
@Getter
@Setter
@ToString(exclude = {"province"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlaceEntity {
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "province_id", nullable = false)
    private ProvinceEntity province;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Double lat;

    private Double lon;

    private Integer views;

    @Column(name = "opening_time")
    private String openingTime;

    private Instant createdAt;

    @PrePersist
    public void handleBeforeCreate() {
        this.createdAt = Instant.now();
        if (this.views == null) {
            this.views = 0;
        }
    }
}
