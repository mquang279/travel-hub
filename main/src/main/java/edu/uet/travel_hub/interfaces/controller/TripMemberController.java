package edu.uet.travel_hub.interfaces.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.uet.travel_hub.application.dto.request.UpdateTripMemberRoleRequest;
import edu.uet.travel_hub.application.dto.response.TripJoinRequestResponse;
import edu.uet.travel_hub.application.dto.response.TripMemberResponse;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.usecases.TripMemberService;
import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/trips/{tripId}")
public class TripMemberController {
    private final TripMemberService tripMemberService;
    private final CurrentUserProvider currentUserProvider;

    public TripMemberController(TripMemberService tripMemberService, CurrentUserProvider currentUserProvider) {
        this.tripMemberService = tripMemberService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/requests")
    public ResponseEntity<List<TripJoinRequestResponse>> getJoinRequests(@PathVariable Long tripId) {
        return ResponseEntity.ok(this.tripMemberService.getJoinRequests(tripId, this.currentUserProvider.getCurrentUserId()));
    }

    @PostMapping("/requests/{userId}/approve")
    public ResponseEntity<TripMemberResponse> approveRequest(@PathVariable Long tripId, @PathVariable Long userId) {
        return ResponseEntity.ok(this.tripMemberService.approveRequest(tripId, userId, this.currentUserProvider.getCurrentUserId()));
    }

    @PostMapping("/requests/{userId}/reject")
    public ResponseEntity<TripMemberResponse> rejectRequest(@PathVariable Long tripId, @PathVariable Long userId) {
        return ResponseEntity.ok(this.tripMemberService.rejectRequest(tripId, userId, this.currentUserProvider.getCurrentUserId()));
    }

    @DeleteMapping("/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long tripId, @PathVariable Long userId) {
        this.tripMemberService.removeMember(tripId, userId, this.currentUserProvider.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/leave")
    public ResponseEntity<Void> leaveTrip(@PathVariable Long tripId) {
        this.tripMemberService.leaveTrip(tripId, this.currentUserProvider.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/members/{userId}/role")
    public ResponseEntity<TripMemberResponse> updateMemberRole(
            @PathVariable Long tripId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateTripMemberRoleRequest request) {
        return ResponseEntity.ok(this.tripMemberService.updateMemberRole(
                tripId,
                userId,
                this.currentUserProvider.getCurrentUserId(),
                request));
    }
}