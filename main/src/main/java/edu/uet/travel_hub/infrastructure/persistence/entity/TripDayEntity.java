package edu.uet.travel_hub.infrastructure.persistence.entity;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Index;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
        name = "trip_days",
        indexes = {
                @Index(name = "idx_trip_days_trip_id_date_id", columnList = "trip_id, date, id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_trip_day_trip_date", columnNames = {"trip_id", "date"})
        })
@Getter
@Setter
@ToString(exclude = {"trip", "activities"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripDayEntity {
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private TripEntity trip;

    @Column(nullable = false)
    private java.time.LocalDate date;

    @Column(nullable = false)
    private int dayNumber;

    @Builder.Default
    @OneToMany(mappedBy = "tripDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC, id ASC")
    @BatchSize(size = 100)
    private List<TripActivityEntity> activities = new ArrayList<>();
}
