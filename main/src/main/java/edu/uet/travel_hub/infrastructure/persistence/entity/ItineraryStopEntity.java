package edu.uet.travel_hub.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "itinerary_stops")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryStopEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "day_id", nullable = false)
    private ItineraryDayEntity day;

    @Column(nullable = false)
    private int sortOrder;

    @Column(length = 20)
    private String startTime;

    @Column(length = 20)
    private String endTime;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 200)
    private String placeName;

    @Column(length = 2000)
    private String note;

    @Column(length = 500)
    private String transportToNext;

    @Column(length = 100)
    private String estimatedCost;

    @Column(nullable = false)
    private boolean highlighted;
}
