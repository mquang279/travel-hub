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
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "travel_place_images")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelPlaceImageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "place_id", nullable = false)
    private TravelPlaceEntity place;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "is_main")
    private boolean main;

    private Instant createdAt;

    @PrePersist
    public void handleBeforeCreate() {
        this.createdAt = Instant.now();
    }
}
