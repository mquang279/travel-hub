package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.uet.travel_hub.infrastructure.persistence.entity.TravelPlaceViewHistoryEntity;

@Repository
public interface TravelPlaceViewHistoryJpaRepository extends JpaRepository<TravelPlaceViewHistoryEntity, Long> {
    @EntityGraph(attributePaths = { "place", "place.province" })
    Page<TravelPlaceViewHistoryEntity> findByUserIdOrderByViewedAtDescIdDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = { "place" })
    List<TravelPlaceViewHistoryEntity> findTop20ByUserIdOrderByViewedAtDescIdDesc(Long userId);
}
