package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.uet.travel_hub.infrastructure.persistence.entity.TravelPlaceReviewEntity;

@Repository
public interface TravelPlaceReviewJpaRepository extends JpaRepository<TravelPlaceReviewEntity, Long> {
    @EntityGraph(attributePaths = { "user" })
    Page<TravelPlaceReviewEntity> findByPlaceIdOrderByUpdatedAtDescIdDesc(Long placeId, Pageable pageable);

    @EntityGraph(attributePaths = { "user" })
    Optional<TravelPlaceReviewEntity> findByPlaceIdAndUserId(Long placeId, Long userId);

    @Query("""
            select coalesce(avg(r.rating), 0) as averageRating,
                   count(r) as reviewCount
            from TravelPlaceReviewEntity r
            where r.place.id = :placeId
            """)
    TravelPlaceReviewStatsProjection getStatsByPlaceId(@Param("placeId") Long placeId);

    @Query("""
            select r.place.id as placeId,
                   coalesce(avg(r.rating), 0) as averageRating,
                   count(r) as reviewCount
            from TravelPlaceReviewEntity r
            where r.place.id in :placeIds
            group by r.place.id
            """)
    List<TravelPlaceReviewStatsProjection> getStatsByPlaceIds(@Param("placeIds") Collection<Long> placeIds);
}
