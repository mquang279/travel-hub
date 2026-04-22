package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import edu.uet.travel_hub.infrastructure.persistence.entity.ProvinceEntity;
import java.util.List;

@Repository
public interface ProvinceJpaRepository extends JpaRepository<ProvinceEntity, Long> {
    List<ProvinceEntity> findAllByOrderByNameAsc();
}
