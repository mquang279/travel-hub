package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.uet.travel_hub.infrastructure.persistence.entity.PaymentProofEntity;

public interface PaymentProofJpaRepository extends JpaRepository<PaymentProofEntity, Long> {
    List<PaymentProofEntity> findBySettlementIdOrderByUploadedAtDesc(Long settlementId);
}
