package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.uet.travel_hub.infrastructure.persistence.entity.TravelPlaceImageEntity;

@Repository
public interface TravelPlaceImageJpaRepository extends JpaRepository<TravelPlaceImageEntity, Long> {
    List<TravelPlaceImageEntity> findByPlaceIdOrderByMainDescIdAsc(Long placeId);

    List<TravelPlaceImageEntity> findByPlaceIdInOrderByPlaceIdAscMainDescIdAsc(Collection<Long> placeIds);

    void deleteByPlaceId(Long placeId);
}
