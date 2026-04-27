package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.uet.travel_hub.infrastructure.persistence.entity.ItineraryEntity;

public interface ItineraryJpaRepository extends JpaRepository<ItineraryEntity, Long> {
    List<ItineraryEntity> findByOwnerIdOrderByUpdatedAtDescIdDesc(Long ownerId);

    boolean existsByOwnerIdAndGroupNameIgnoreCase(Long ownerId, String groupName);

    boolean existsByOwnerIdAndGroupNameIgnoreCaseAndIdNot(Long ownerId, String groupName, Long id);

    @EntityGraph(attributePaths = { "days", "days.stops" })
    Optional<ItineraryEntity> findByIdAndOwnerId(Long id, Long ownerId);

    @EntityGraph(attributePaths = { "days", "days.stops" })
    Optional<ItineraryEntity> findByOwnerIdAndGroupNameIgnoreCase(Long ownerId, String groupName);
}
