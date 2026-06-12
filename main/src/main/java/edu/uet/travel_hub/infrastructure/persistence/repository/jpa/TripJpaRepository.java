package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.uet.travel_hub.domain.enums.TripMemberStatus;
import edu.uet.travel_hub.domain.enums.TripStatus;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripEntity;

public interface TripJpaRepository extends JpaRepository<TripEntity, Long> {
    Optional<TripEntity> findByInviteCode(String inviteCode);

    @EntityGraph(attributePaths = {"members", "members.user", "leader"})
    List<TripEntity> findDistinctByMembersUserIdOrderByStartDateAsc(Long userId);

    @EntityGraph(attributePaths = {"members", "members.user", "leader"})
    Optional<TripEntity> findWithMembersById(Long id);

    @Query("""
            select distinct t
            from TripEntity t
            join t.members m
            where t.id = :tripId
              and m.user.id = :userId
              and m.status = :status
            """)
    Optional<TripEntity> findActiveMemberTripById(
            @Param("tripId") Long tripId,
            @Param("userId") Long userId,
            @Param("status") TripMemberStatus status);

    @Query("""
            select distinct t
            from TripEntity t
            join t.members m
            where m.user.id = :userId
              and m.status = :status
              and lower(t.name) = lower(:name)
            order by t.startDate asc, t.id asc
            """)
    List<TripEntity> findActiveMemberTripsByName(
            @Param("userId") Long userId,
            @Param("status") TripMemberStatus status,
            @Param("name") String name);

    @Query(
            value = """
                    select t
                    from TripEntity t
                    where exists (
                        select 1
                        from TripMemberEntity m
                        where m.trip = t
                          and m.user.id = :userId
                          and m.status = :memberStatus
                    )
                      and (t.status = :completedStatus or t.endDate < :today)
                    order by case when t.endDate is null then 1 else 0 end, t.endDate desc, t.id desc
                    """,
            countQuery = """
                    select count(t)
                    from TripEntity t
                    where exists (
                        select 1
                        from TripMemberEntity m
                        where m.trip = t
                          and m.user.id = :userId
                          and m.status = :memberStatus
                    )
                      and (t.status = :completedStatus or t.endDate < :today)
                    """)
    Page<TripEntity> findPastActiveMemberTripsOrderByEndDateDesc(
            @Param("userId") Long userId,
            @Param("memberStatus") TripMemberStatus memberStatus,
            @Param("completedStatus") TripStatus completedStatus,
            @Param("today") java.time.LocalDate today,
            Pageable pageable);
}
