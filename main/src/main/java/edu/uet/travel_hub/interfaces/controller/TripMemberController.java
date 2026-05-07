package edu.uet.travel_hub.interfaces.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.uet.travel_hub.application.dto.response.TripJoinRequestResponse;
import edu.uet.travel_hub.application.dto.response.TripMemberResponse;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.usecases.TripMemberService;

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
}