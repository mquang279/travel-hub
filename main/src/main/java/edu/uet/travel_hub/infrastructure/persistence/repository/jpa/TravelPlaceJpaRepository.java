package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import java.util.Optional;

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

    @Modifying
    @Query("update TravelPlaceEntity p set p.views = coalesce(p.views, 0) + 1 where p.id = :id")
    void incrementViews(@Param("id") Long id);
}
