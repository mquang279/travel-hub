package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.uet.travel_hub.infrastructure.persistence.entity.BankAccountEntity;

public interface BankAccountJpaRepository extends JpaRepository<BankAccountEntity, Long> {
    List<BankAccountEntity> findByUserIdOrderByIsDefaultDescCreatedAtDesc(Long userId);

    Optional<BankAccountEntity> findByIdAndUserId(Long id, Long userId);

    Optional<BankAccountEntity> findFirstByUserIdAndIsDefaultTrue(Long userId);

    boolean existsByUserId(Long userId);
}
