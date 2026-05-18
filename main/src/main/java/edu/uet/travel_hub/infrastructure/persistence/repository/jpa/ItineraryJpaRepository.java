package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.uet.travel_hub.infrastructure.persistence.entity.ItineraryEntity;

public interface ItineraryJpaRepository extends JpaRepository<ItineraryEntity, Long> {
    @Query("""
            select i.id as id,
                   i.groupName as groupName,
                   i.version as version,
                   count(distinct d.id) as totalDays,
                   count(distinct s.id) as totalStops,
                   i.updatedAt as updatedAt
            from ItineraryEntity i
            left join i.days d
            left join d.stops s
            where i.owner.id = :ownerId
            group by i.id, i.groupName, i.version, i.updatedAt
            order by i.updatedAt desc, i.id desc
            """)
    List<ItinerarySummaryProjection> findSummariesByOwnerId(@Param("ownerId") Long ownerId);

    boolean existsByOwnerIdAndGroupNameIgnoreCase(Long ownerId, String groupName);

    boolean existsByOwnerIdAndGroupNameIgnoreCaseAndIdNot(Long ownerId, String groupName, Long id);

    @EntityGraph(attributePaths = { "days" })
    Optional<ItineraryEntity> findByIdAndOwnerId(Long id, Long ownerId);

    @EntityGraph(attributePaths = { "days" })
    Optional<ItineraryEntity> findByOwnerIdAndGroupNameIgnoreCase(Long ownerId, String groupName);

    @Query("""
            select i.id as itineraryId,
                   i.groupName as groupName,
                   i.version as version,
                   i.owner.id as ownerId,
                   i.createdAt as createdAt,
                   i.updatedAt as updatedAt,
                   d.id as dayId,
                   d.dayIndex as dayIndex,
                   d.label as dayLabel,
                   d.dateLabel as dateLabel,
                   s.id as stopId,
                   s.sortOrder as sortOrder,
                   s.startTime as startTime,
                   s.endTime as endTime,
                   s.title as title,
                   s.placeName as placeName,
                   s.note as note,
                   s.transportToNext as transportToNext,
                   s.estimatedCost as estimatedCost,
                   s.colorHex as colorHex,
                   s.iconName as iconName
            from ItineraryEntity i
            left join i.days d
            left join d.stops s
            where i.id = :id and i.owner.id = :ownerId
            order by d.dayIndex asc, d.id asc, s.sortOrder asc, s.id asc
            """)
    List<ItineraryDetailRow> findDetailRowsByIdAndOwnerId(@Param("id") Long id, @Param("ownerId") Long ownerId);

    @Query("""
            select i.id as itineraryId,
                   i.groupName as groupName,
                   i.version as version,
                   i.owner.id as ownerId,
                   i.createdAt as createdAt,
                   i.updatedAt as updatedAt,
                   d.id as dayId,
                   d.dayIndex as dayIndex,
                   d.label as dayLabel,
                   d.dateLabel as dateLabel,
                   s.id as stopId,
                   s.sortOrder as sortOrder,
                   s.startTime as startTime,
                   s.endTime as endTime,
                   s.title as title,
                   s.placeName as placeName,
                   s.note as note,
                   s.transportToNext as transportToNext,
                   s.estimatedCost as estimatedCost,
                   s.colorHex as colorHex,
                   s.iconName as iconName
            from ItineraryEntity i
            left join i.days d
            left join d.stops s
            where i.owner.id = :ownerId and lower(i.groupName) = lower(:groupName)
            order by d.dayIndex asc, d.id asc, s.sortOrder asc, s.id asc
            """)
    List<ItineraryDetailRow> findDetailRowsByOwnerIdAndGroupName(
            @Param("ownerId") Long ownerId,
            @Param("groupName") String groupName);
}
