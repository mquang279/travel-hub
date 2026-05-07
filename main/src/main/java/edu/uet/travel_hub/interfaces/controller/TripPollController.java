package edu.uet.travel_hub.interfaces.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.uet.travel_hub.application.dto.request.CreateTripPollRequest;
import edu.uet.travel_hub.application.dto.response.TripPollResponse;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.usecases.TripPollService;
import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/trips/{tripId}/polls")
public class TripPollController {
    private final TripPollService tripPollService;
    private final CurrentUserProvider currentUserProvider;

    public TripPollController(TripPollService tripPollService, CurrentUserProvider currentUserProvider) {
        this.tripPollService = tripPollService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("")
    public ResponseEntity<List<TripPollResponse>> listPolls(@PathVariable Long tripId) {
        return ResponseEntity.ok(this.tripPollService.listPolls(tripId, this.currentUserProvider.getCurrentUserId()));
    }

    @PostMapping("")
    public ResponseEntity<TripPollResponse> createPoll(@PathVariable Long tripId, @Valid @RequestBody CreateTripPollRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(this.tripPollService.createPoll(tripId, this.currentUserProvider.getCurrentUserId(), request));
    }

    @PostMapping("/{pollId}/vote")
    public ResponseEntity<List<TripPollResponse>> toggleVote(@PathVariable Long tripId, @PathVariable Long pollId) {
        return ResponseEntity.ok(this.tripPollService.toggleVote(tripId, pollId, this.currentUserProvider.getCurrentUserId()));
    }
}