package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.uet.travel_hub.infrastructure.persistence.entity.ExpenseSplitEntity;

public interface ExpenseSplitJpaRepository extends JpaRepository<ExpenseSplitEntity, Long> {
    List<ExpenseSplitEntity> findByExpenseTripId(Long tripId);

    void deleteByExpenseId(Long expenseId);
}
