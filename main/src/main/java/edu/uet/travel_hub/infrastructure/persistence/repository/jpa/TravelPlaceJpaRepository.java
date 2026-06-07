package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.uet.travel_hub.infrastructure.persistence.entity.TravelPlaceEntity;

@Repository
public interface TravelPlaceJpaRepository extends JpaRepository<TravelPlaceEntity, Long> {
    @EntityGraph(attributePaths = { "province" })
    @Query("""
            select p
            from TravelPlaceEntity p
            where (:provinceId is null or p.province.id = :provinceId)
              and (:keyword is null or :keyword = ''
                   or lower(p.name) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(p.description, '')) like lower(concat('%', :keyword, '%')))
            """)
    Page<TravelPlaceEntity> search(@Param("provinceId") Long provinceId, @Param("keyword") String keyword,
            Pageable pageable);

    @EntityGraph(attributePaths = { "province" })
    @Query(value = """
            select p
            from TravelPlaceEntity p
            join p.province province
            where :keyword is null or :keyword = ''
               or lower(p.name) like lower(concat('%', :keyword, '%'))
               or lower(province.name) like lower(concat('%', :keyword, '%'))
            """, countQuery = """
            select count(p)
            from TravelPlaceEntity p
            join p.province province
            where :keyword is null or :keyword = ''
               or lower(p.name) like lower(concat('%', :keyword, '%'))
               or lower(province.name) like lower(concat('%', :keyword, '%'))
            """)
    Page<TravelPlaceEntity> searchByNameOrProvinceName(@Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = { "province" })
    @Query(value = """
            select p
            from TravelPlaceEntity p
            where (:provinceId is null or p.province.id = :provinceId)
            order by function('random')
            """, countQuery = """
            select count(p)
            from TravelPlaceEntity p
            where (:provinceId is null or p.province.id = :provinceId)
            """)
    Page<TravelPlaceEntity> findRandom(@Param("provinceId") Long provinceId, Pageable pageable);

    @Query(value = """
            SELECT place.id
            FROM travel_places place
            LEFT JOIN (
                SELECT travel_place_id, COUNT(*) AS post_count
                FROM posts
                WHERE travel_place_id IS NOT NULL
                GROUP BY travel_place_id
            ) post_stats ON post_stats.travel_place_id = place.id
            LEFT JOIN (
                SELECT place_id, COUNT(*) AS trip_count
                FROM trips
                WHERE place_id IS NOT NULL
                GROUP BY place_id
            ) trip_stats ON trip_stats.place_id = place.id
            LEFT JOIN (
                SELECT place_id,
                       AVG(rating) AS average_rating,
                       COUNT(*) AS review_count
                FROM travel_place_reviews
                GROUP BY place_id
            ) review_stats ON review_stats.place_id = place.id
            WHERE COALESCE(post_stats.post_count, 0) + COALESCE(trip_stats.trip_count, 0) > 0
            ORDER BY COALESCE(review_stats.average_rating, 0) DESC,
                     COALESCE(post_stats.post_count, 0) + COALESCE(trip_stats.trip_count, 0) DESC,
                     COALESCE(review_stats.review_count, 0) DESC,
                     place.id ASC
            """, nativeQuery = true)
    List<Long> findFeaturedPlaceIds(Pageable pageable);

    @Modifying
    @Query("update TravelPlaceEntity p set p.views = coalesce(p.views, 0) + 1 where p.id = :id")
    void incrementViews(@Param("id") Long id);

    @EntityGraph(attributePaths = { "province" })
    List<TravelPlaceEntity> findByIdIn(Collection<Long> ids);
}
