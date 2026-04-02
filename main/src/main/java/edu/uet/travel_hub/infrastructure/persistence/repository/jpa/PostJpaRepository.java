package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.uet.travel_hub.infrastructure.persistence.entity.PostJpaEntity;

public interface PostJpaRepository extends JpaRepository<PostJpaEntity, Long> {

}
