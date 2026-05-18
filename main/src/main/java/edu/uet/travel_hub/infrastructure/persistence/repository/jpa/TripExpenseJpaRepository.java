package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.uet.travel_hub.infrastructure.persistence.entity.TripExpenseEntity;

public interface TripExpenseJpaRepository extends JpaRepository<TripExpenseEntity, Long> {
    List<TripExpenseEntity> findByTripIdOrderByExpenseDateDescIdDesc(Long tripId);

    Optional<TripExpenseEntity> findByIdAndTripId(Long id, Long tripId);

    @Query("SELECT SUM(e.amount) FROM TripExpenseEntity e WHERE e.trip.id = :tripId")
    Optional<BigDecimal> sumAmountByTripId(@Param("tripId") Long tripId);

    void deleteByTripId(Long tripId);
}