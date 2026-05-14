package edu.uet.travel_hub.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;

import edu.uet.travel_hub.domain.enums.TripExpenseCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
@Table(name = "trip_expenses")
@Getter
@Setter
@ToString(exclude = {"trip", "paidBy"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripExpenseEntity {
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private TripEntity trip;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TripExpenseCategory category;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by_user_id", nullable = false)
    private UserEntity paidBy;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false)
    private Instant expenseDate;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void handleBeforeCreate() {
        Instant now = Instant.now();
        this.expenseDate = now;
        this.createdAt = now;
    }
}