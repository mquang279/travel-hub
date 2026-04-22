package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.uet.travel_hub.infrastructure.persistence.entity.WardEntity;

@Repository
public interface WardJpaRepository extends JpaRepository<WardEntity, Long> {
    List<WardEntity> findByDistrictIdOrderByNameAsc(Long districtId);
}
