package edu.uet.travel_hub.interfaces.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
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

import edu.uet.travel_hub.application.dto.request.CreateTripActivityRequest;
import edu.uet.travel_hub.application.dto.request.UpdateTripActivityRequest;
import edu.uet.travel_hub.application.dto.response.TripActivityResponse;
import edu.uet.travel_hub.application.dto.response.TripDayResponse;
import edu.uet.travel_hub.application.port.in.TripActivityUseCase;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/trips/{tripId}")
public class TripActivityController {
    private final TripActivityUseCase tripActivityUseCase;
    private final CurrentUserProvider currentUserProvider;

    public TripActivityController(TripActivityUseCase tripActivityUseCase, CurrentUserProvider currentUserProvider) {
        this.tripActivityUseCase = tripActivityUseCase;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/days")
    public ResponseEntity<List<TripDayResponse>> listTripDays(@PathVariable Long tripId) {
        return ResponseEntity.ok(
                this.tripActivityUseCase.listTripDays(tripId, this.currentUserProvider.getCurrentUserId()));
    }

    @PostMapping("/activities")
    public ResponseEntity<TripActivityResponse> createActivity(
            @PathVariable Long tripId,
            @Valid @RequestBody CreateTripActivityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(this.tripActivityUseCase.createActivity(
                        tripId,
                        this.currentUserProvider.getCurrentUserId(),
                        request));
    }

    @PutMapping("/activities/{activityId}")
    public ResponseEntity<TripActivityResponse> updateActivity(
            @PathVariable Long tripId,
            @PathVariable Long activityId,
            @Valid @RequestBody UpdateTripActivityRequest request) {
        return ResponseEntity.ok(this.tripActivityUseCase.updateActivity(
                tripId,
                activityId,
                this.currentUserProvider.getCurrentUserId(),
                request));
    }

    @DeleteMapping("/activities/{activityId}")
    public ResponseEntity<Void> deleteActivity(@PathVariable Long tripId, @PathVariable Long activityId) {
        this.tripActivityUseCase.deleteActivity(tripId, activityId, this.currentUserProvider.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }
}
