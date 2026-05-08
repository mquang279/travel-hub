package edu.uet.travel_hub.application.usecases;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.application.dto.request.CreateTripPollRequest;
import edu.uet.travel_hub.application.dto.request.UpdateTripPollRequest;
import edu.uet.travel_hub.application.dto.response.TripPollResponse;
import edu.uet.travel_hub.application.exception.ForbiddenTripActionException;
import edu.uet.travel_hub.application.exception.ResourceNotFoundException;
import edu.uet.travel_hub.domain.enums.TripMemberStatus;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripPollEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripPollVoteEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.PollVoteCount;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripPollJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripPollVoteJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripMemberJpaRepository;

@Service
public class TripPollService {
    private final TripService tripService;
    private final TripPollJpaRepository tripPollJpaRepository;
    private final TripPollVoteJpaRepository tripPollVoteJpaRepository;
    private final TripMemberJpaRepository tripMemberJpaRepository;
    private final TripActivityLogService tripActivityLogService;

    public TripPollService(
            TripService tripService,
            TripPollJpaRepository tripPollJpaRepository,
            TripPollVoteJpaRepository tripPollVoteJpaRepository,
            TripMemberJpaRepository tripMemberJpaRepository,
            TripActivityLogService tripActivityLogService) {
        this.tripService = tripService;
        this.tripPollJpaRepository = tripPollJpaRepository;
        this.tripPollVoteJpaRepository = tripPollVoteJpaRepository;
        this.tripMemberJpaRepository = tripMemberJpaRepository;
        this.tripActivityLogService = tripActivityLogService;
    }

    @Transactional(readOnly = true)
    public List<TripPollResponse> listPolls(Long tripId, Long currentUserId) {
        this.tripService.requireActiveMemberTrip(tripId, currentUserId);
        List<TripPollEntity> polls = this.tripPollJpaRepository.findByTripIdOrderByCreatedAtDesc(tripId);
        List<PollVoteCount> results = this.tripPollVoteJpaRepository.countVotesByPollForTrip(tripId);
        Map<Long, Long> voteCounts = results.stream()
                .collect(Collectors.toMap(PollVoteCount::getPollId, PollVoteCount::getCount));

        // Preserve existing behavior: polls without votes must still be present with count=0.
        polls.forEach(poll -> voteCounts.putIfAbsent(poll.getId(), 0L));

        long maxVotes = voteCounts.values().stream().max(Long::compareTo).orElse(0L);
        return polls.stream()
                .map(poll -> toResponse(poll, currentUserId, voteCounts, maxVotes))
                .toList();
    }

    @Transactional
    public TripPollResponse createPoll(Long tripId, Long currentUserId, CreateTripPollRequest request) {
        TripEntity trip = this.tripService.requireActiveMemberTrip(tripId, currentUserId);
        UserEntity user = this.tripService.findUser(currentUserId);
        TripPollEntity poll = this.tripPollJpaRepository.save(TripPollEntity.builder()
                .trip(trip)
                .title(request.title().trim())
                .category(request.category())
                .createdBy(user)
                .build());
        this.tripActivityLogService.log(trip, user, "CREATE_POLL", "POLL", poll.getId(), "poll created");
        return toResponse(poll, currentUserId, Map.of(), 0L);
    }

    @Transactional
    public List<TripPollResponse> toggleVote(Long tripId, Long pollId, Long currentUserId) {
        TripEntity trip = this.tripService.requireActiveMemberTrip(tripId, currentUserId);
        TripPollEntity poll = this.tripPollJpaRepository.findByIdAndTripId(pollId, tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Poll not found"));
                if (poll.isClosed()) {
                        throw new ForbiddenTripActionException("Poll is closed");
                }
        UserEntity user = this.tripService.findUser(currentUserId);
        this.tripMemberJpaRepository.findByTripIdAndUserId(tripId, currentUserId)
                .filter(member -> member.getStatus() == TripMemberStatus.ACTIVE)
                .orElseThrow(() -> new ForbiddenTripActionException("Only approved members can vote"));

        this.tripPollVoteJpaRepository.findByPollIdAndUserId(pollId, currentUserId)
                .ifPresentOrElse(
                        this.tripPollVoteJpaRepository::delete,
                        () -> this.tripPollVoteJpaRepository.save(TripPollVoteEntity.builder()
                                .poll(poll)
                                .user(user)
                                .build()));

        this.tripActivityLogService.log(trip, user, "TOGGLE_VOTE", "POLL", pollId, "vote toggled");
        return listPolls(tripId, currentUserId);
    }

        @Transactional
        public TripPollResponse updatePoll(Long tripId, Long pollId, Long currentUserId, UpdateTripPollRequest request) {
                this.tripService.requireLeaderTrip(tripId, currentUserId);
                TripPollEntity poll = this.tripPollJpaRepository.findByIdAndTripId(pollId, tripId)
                                .orElseThrow(() -> new ResourceNotFoundException("Poll not found"));
                poll.setTitle(request.title().trim());
                poll.setCategory(request.category());
                TripPollEntity saved = this.tripPollJpaRepository.save(poll);
                this.tripActivityLogService.log(saved.getTrip(), this.tripService.findUser(currentUserId), "UPDATE_POLL", "POLL", pollId, "poll updated");
                return toResponse(saved, currentUserId, Map.of(), 0L);
        }

        @Transactional
        public TripPollResponse closePoll(Long tripId, Long pollId, Long currentUserId) {
                this.tripService.requireLeaderTrip(tripId, currentUserId);
                TripPollEntity poll = this.tripPollJpaRepository.findByIdAndTripId(pollId, tripId)
                                .orElseThrow(() -> new ResourceNotFoundException("Poll not found"));
                poll.setClosed(true);
                TripPollEntity saved = this.tripPollJpaRepository.save(poll);
                this.tripActivityLogService.log(saved.getTrip(), this.tripService.findUser(currentUserId), "CLOSE_POLL", "POLL", pollId, "poll closed");
                return toResponse(saved, currentUserId, Map.of(), 0L);
        }

        @Transactional
        public void deletePoll(Long tripId, Long pollId, Long currentUserId) {
                this.tripService.requireLeaderTrip(tripId, currentUserId);
                TripPollEntity poll = this.tripPollJpaRepository.findByIdAndTripId(pollId, tripId)
                                .orElseThrow(() -> new ResourceNotFoundException("Poll not found"));
                this.tripPollJpaRepository.delete(poll);
                this.tripActivityLogService.log(poll.getTrip(), this.tripService.findUser(currentUserId), "DELETE_POLL", "POLL", pollId, "poll deleted");
        }

    private TripPollResponse toResponse(TripPollEntity poll, Long currentUserId, Map<Long, Long> voteCounts, long maxVotes) {
        int votesCount = voteCounts.getOrDefault(poll.getId(), 0L).intValue();
        boolean hasVoted = this.tripPollVoteJpaRepository.existsByPollIdAndUserId(poll.getId(), currentUserId);
        boolean winning = votesCount > 0 && votesCount == maxVotes;
        List<String> voters = poll.getVotes().stream()
                .map(vote -> vote.getUser().getAvatarUrl())
                .filter(value -> value != null && !value.isBlank())
                .toList();
        return new TripPollResponse(
                poll.getId(),
                poll.getTitle(),
                poll.getCategory(),
                votesCount,
                winning,
                hasVoted,
                voters);
    }
}