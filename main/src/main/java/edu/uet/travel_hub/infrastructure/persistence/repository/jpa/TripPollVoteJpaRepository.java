package edu.uet.travel_hub.infrastructure.persistence.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.uet.travel_hub.infrastructure.persistence.entity.TripPollVoteEntity;

import org.springframework.data.jpa.repository.Modifying;

public interface TripPollVoteJpaRepository extends JpaRepository<TripPollVoteEntity, Long> {
    Optional<TripPollVoteEntity> findByPollIdAndUserId(Long pollId, Long userId);

    boolean existsByPollIdAndUserId(Long pollId, Long userId);

    long countByPollId(Long pollId);

    List<TripPollVoteEntity> findByPollTripId(Long tripId);

    @Query("SELECT v.poll.id AS pollId, COUNT(v) AS count " +
            "FROM TripPollVoteEntity v WHERE v.poll.trip.id = :tripId " +
            "GROUP BY v.poll.id")
    List<PollVoteCount> countVotesByPollForTrip(@Param("tripId") Long tripId);

    @Modifying
    @Query("DELETE FROM TripPollVoteEntity v WHERE v.poll.trip.id = :tripId")
    void deleteVotesByTripId(@Param("tripId") Long tripId);
}