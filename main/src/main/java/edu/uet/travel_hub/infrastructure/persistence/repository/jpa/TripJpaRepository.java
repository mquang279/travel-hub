package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.uet.travel_hub.infrastructure.persistence.entity.TripEntity;

public interface TripJpaRepository extends JpaRepository<TripEntity, Long> {
    Optional<TripEntity> findByInviteCode(String inviteCode);

    @EntityGraph(attributePaths = {"members", "members.user", "leader"})
    List<TripEntity> findDistinctByMembersUserIdOrderByStartDateAsc(Long userId);

    @EntityGraph(attributePaths = {"members", "members.user", "leader"})
    Optional<TripEntity> findWithMembersById(Long id);
}