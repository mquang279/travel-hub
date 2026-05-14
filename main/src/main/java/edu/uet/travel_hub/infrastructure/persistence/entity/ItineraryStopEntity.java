package edu.uet.travel_hub.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "itinerary_stops")
@Getter
@Setter
@ToString(exclude = {"day"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryStopEntity {
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
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

    @Column
    private Long colorHex;

    @Column(length = 100)
    private String iconName;
}
