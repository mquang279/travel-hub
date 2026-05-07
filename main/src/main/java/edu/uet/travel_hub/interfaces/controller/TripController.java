package edu.uet.travel_hub.interfaces.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.uet.travel_hub.application.dto.request.CreateTripRequest;
import edu.uet.travel_hub.application.dto.response.TripDashboardResponse;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.usecases.TripService;
import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api")
public class TripController {
    private final TripService tripService;
    private final CurrentUserProvider currentUserProvider;

    public TripController(TripService tripService, CurrentUserProvider currentUserProvider) {
        this.tripService = tripService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/users/me/dashboard")
    public ResponseEntity<TripDashboardResponse> getDashboard() {
        return ResponseEntity.ok(this.tripService.getDashboard(this.currentUserProvider.getCurrentUserId()));
    }

    @GetMapping("/trips/{tripId}")
    public ResponseEntity<Object> getTripDetail(@PathVariable Long tripId) {
        return ResponseEntity.ok().body(null);
    }

    @PostMapping("/trips")
    public ResponseEntity<Long> createTrip(@Valid @RequestBody CreateTripRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(this.tripService.createTrip(this.currentUserProvider.getCurrentUserId(), request));
    }
}