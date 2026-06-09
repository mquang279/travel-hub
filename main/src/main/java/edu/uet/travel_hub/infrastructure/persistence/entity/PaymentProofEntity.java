package edu.uet.travel_hub.infrastructure.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "payment_proofs")
@Getter
@Setter
@ToString(exclude = {"settlement", "uploadedBy"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProofEntity {
    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private SettlementEntity settlement;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id", nullable = false)
    private UserEntity uploadedBy;

    @Column(nullable = false, length = 1000)
    private String imageUrl;

    @Column(length = 1000)
    private String note;

    @Column(nullable = false, updatable = false)
    private Instant uploadedAt;

    @PrePersist
    public void onCreate() {
        this.uploadedAt = Instant.now();
    }
}
