package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.uet.travel_hub.infrastructure.persistence.entity.DistrictEntity;

@Repository
public interface DistrictJpaRepository extends JpaRepository<DistrictEntity, Long> {
    List<DistrictEntity> findByProvinceIdOrderByNameAsc(Long provinceId);
}
