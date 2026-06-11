package edu.uet.travel_hub.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import edu.uet.travel_hub.domain.enums.ExpenseSource;
import edu.uet.travel_hub.domain.enums.TripExpenseCategory;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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

    @Column(length = 1000)
    private String note;

    @Column(name = "raw_ocr_text", columnDefinition = "TEXT")
    private String rawOcrText;

    @Column(name = "proof_image_url", length = 1000)
    private String proofImageUrl;

    @Builder.Default
    @Column(name = "split_type", nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'EQUAL'")
    private String splitType = "EQUAL";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpenseSource source;

    @Column(nullable = false)
    private Instant expenseDate;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExpenseSplitEntity> splits = new ArrayList<>();

    @PrePersist
    public void handleBeforeCreate() {
        Instant now = Instant.now();
        if (this.expenseDate == null) {
            this.expenseDate = now;
        }
        this.createdAt = now;
        this.updatedAt = now;
        if (this.source == null) {
            this.source = ExpenseSource.MANUAL;
        }
        if (this.splitType == null || this.splitType.isBlank()) {
            this.splitType = "EQUAL";
        }
    }

    @PreUpdate
    public void handleBeforeUpdate() {
        this.updatedAt = Instant.now();
        if (this.splitType == null || this.splitType.isBlank()) {
            this.splitType = "EQUAL";
        }
    }
}
