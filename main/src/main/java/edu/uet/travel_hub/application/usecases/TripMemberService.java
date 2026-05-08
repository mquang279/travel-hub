package edu.uet.travel_hub.application.usecases;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.application.dto.request.UpdateTripMemberRoleRequest;
import edu.uet.travel_hub.application.dto.response.TripJoinRequestResponse;
import edu.uet.travel_hub.application.dto.response.TripMemberResponse;
import edu.uet.travel_hub.application.exception.ForbiddenTripActionException;
import edu.uet.travel_hub.application.exception.ResourceNotFoundException;
import edu.uet.travel_hub.domain.enums.TripMemberRole;
import edu.uet.travel_hub.domain.enums.TripMemberStatus;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripMemberEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripMemberJpaRepository;

@Service
public class TripMemberService {
    private final TripService tripService;
    private final TripMemberJpaRepository tripMemberJpaRepository;
    private final TripActivityLogService tripActivityLogService;

    public TripMemberService(
            TripService tripService,
            TripMemberJpaRepository tripMemberJpaRepository,
            TripActivityLogService tripActivityLogService) {
        this.tripService = tripService;
        this.tripMemberJpaRepository = tripMemberJpaRepository;
        this.tripActivityLogService = tripActivityLogService;
    }

    @Transactional(readOnly = true)
    public List<TripJoinRequestResponse> getJoinRequests(Long tripId, Long currentUserId) {
        TripEntity trip = this.tripService.requireLeaderTrip(tripId, currentUserId);
        return this.tripMemberJpaRepository.findByTripIdAndStatusOrderByRequestedAtAsc(trip.getId(), TripMemberStatus.PENDING)
                .stream()
                .map(this::toJoinRequestResponse)
                .toList();
    }

    @Transactional
    public TripMemberResponse approveRequest(Long tripId, Long requesterUserId, Long currentUserId) {
        TripEntity trip = this.tripService.requireLeaderTrip(tripId, currentUserId);
        TripMemberEntity member = this.tripMemberJpaRepository.findByTripIdAndUserId(tripId, requesterUserId)
                .orElseThrow(() -> new ForbiddenTripActionException("Join request not found"));
        if (member.getStatus() != TripMemberStatus.PENDING) {
            throw new ForbiddenTripActionException("Join request is not pending");
        }

        member.setStatus(TripMemberStatus.ACTIVE);
        member.setRole(TripMemberRole.MEMBER);
        member.setRespondedAt(java.time.Instant.now());
        TripMemberEntity saved = this.tripMemberJpaRepository.save(member);
        this.tripActivityLogService.log(trip, trip.getOwner(), "APPROVE_MEMBER", "USER", requesterUserId, "member approved");
        return toMemberResponse(saved);
    }

    @Transactional
    public TripMemberResponse rejectRequest(Long tripId, Long requesterUserId, Long currentUserId) {
        TripEntity trip = this.tripService.requireLeaderTrip(tripId, currentUserId);
        TripMemberEntity member = this.tripMemberJpaRepository.findByTripIdAndUserId(tripId, requesterUserId)
                .orElseThrow(() -> new ForbiddenTripActionException("Join request not found"));
        if (member.getStatus() != TripMemberStatus.PENDING) {
            throw new ForbiddenTripActionException("Join request is not pending");
        }

        member.setStatus(TripMemberStatus.REJECTED);
        member.setRespondedAt(java.time.Instant.now());
        TripMemberEntity saved = this.tripMemberJpaRepository.save(member);
        this.tripActivityLogService.log(trip, trip.getOwner(), "REJECT_MEMBER", "USER", requesterUserId, "member rejected");
        return toMemberResponse(saved);
    }

    @Transactional
    public void removeMember(Long tripId, Long memberUserId, Long currentUserId) {
        TripEntity trip = this.tripService.requireLeaderTrip(tripId, currentUserId);
        TripMemberEntity member = this.tripMemberJpaRepository.findByTripIdAndUserId(tripId, memberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (member.getStatus() != TripMemberStatus.ACTIVE) {
            throw new ForbiddenTripActionException("Member is not active");
        }
        if (member.getRole() == TripMemberRole.LEADER || memberUserId.equals(currentUserId)) {
            throw new ForbiddenTripActionException("Leader cannot remove this member");
        }

        this.tripMemberJpaRepository.delete(member);
        this.tripActivityLogService.log(trip, trip.getOwner(), "REMOVE_MEMBER", "USER", memberUserId, "member removed");
    }

    @Transactional
    public void leaveTrip(Long tripId, Long currentUserId) {
        TripEntity trip = this.tripService.requireActiveMemberTrip(tripId, currentUserId);
        TripMemberEntity currentMember = this.tripMemberJpaRepository.findByTripIdAndUserId(tripId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        if (currentMember.getRole() == TripMemberRole.LEADER) {
            throw new ForbiddenTripActionException("Leader must transfer role before leaving");
        }

        this.tripMemberJpaRepository.delete(currentMember);
        this.tripActivityLogService.log(trip, currentMember.getUser(), "LEAVE_TRIP", "USER", currentUserId, "member left trip");
    }

    @Transactional
    public TripMemberResponse updateMemberRole(Long tripId, Long memberUserId, Long currentUserId, UpdateTripMemberRoleRequest request) {
        TripEntity trip = this.tripService.requireLeaderTrip(tripId, currentUserId);
        TripMemberEntity targetMember = this.tripMemberJpaRepository.findByTripIdAndUserId(tripId, memberUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (targetMember.getStatus() != TripMemberStatus.ACTIVE) {
            throw new ForbiddenTripActionException("Member is not active");
        }

        if (request.role() == TripMemberRole.LEADER) {
            TripMemberEntity currentLeaderMember = this.tripMemberJpaRepository.findByTripIdAndUserId(tripId, currentUserId)
                    .orElseThrow(() -> new ForbiddenTripActionException("Current leader not found"));
            if (currentLeaderMember.getStatus() != TripMemberStatus.ACTIVE || currentLeaderMember.getRole() != TripMemberRole.LEADER) {
                throw new ForbiddenTripActionException("Current user is not leader");
            }
            currentLeaderMember.setRole(TripMemberRole.MEMBER);
            targetMember.setRole(TripMemberRole.LEADER);
            trip.setLeader(targetMember.getUser());
            this.tripMemberJpaRepository.save(currentLeaderMember);
        } else {
            targetMember.setRole(TripMemberRole.MEMBER);
        }

        TripMemberEntity saved = this.tripMemberJpaRepository.save(targetMember);
        this.tripActivityLogService.log(trip, trip.getOwner(), "UPDATE_MEMBER_ROLE", "USER", memberUserId, "member role updated");
        return toMemberResponse(saved);
    }

    private TripJoinRequestResponse toJoinRequestResponse(TripMemberEntity member) {
        return new TripJoinRequestResponse(
                member.getUser().getId(),
                displayName(member),
                member.getUser().getAvatarUrl(),
                member.getRequestedAt());
    }

    private TripMemberResponse toMemberResponse(TripMemberEntity member) {
        return new TripMemberResponse(
                member.getUser().getId(),
                displayName(member),
                member.getUser().getAvatarUrl());
    }

    private String displayName(TripMemberEntity member) {
        if (member.getUser().getName() != null && !member.getUser().getName().isBlank()) {
            return member.getUser().getName();
        }
        return member.getUser().getUsername();
    }
}