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

import edu.uet.travel_hub.application.dto.request.CreateItineraryDayRequest;
import edu.uet.travel_hub.application.dto.request.CreateItineraryRequest;
import edu.uet.travel_hub.application.dto.request.CreateItineraryStopRequest;
import edu.uet.travel_hub.application.dto.request.UpdateItineraryDayRequest;
import edu.uet.travel_hub.application.dto.request.UpdateItineraryRequest;
import edu.uet.travel_hub.application.dto.request.UpdateItineraryStopRequest;
import edu.uet.travel_hub.application.dto.response.ItineraryResponse;
import edu.uet.travel_hub.application.dto.response.ItinerarySummaryResponse;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.usecases.ItineraryService;
import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/itineraries")
public class ItineraryController {
    private final ItineraryService itineraryService;
    private final CurrentUserProvider currentUserProvider;

    public ItineraryController(ItineraryService itineraryService, CurrentUserProvider currentUserProvider) {
        this.itineraryService = itineraryService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("")
    public ResponseEntity<List<ItinerarySummaryResponse>> getMyItineraries() {
        return ResponseEntity.ok(this.itineraryService.getMyItineraries(this.currentUserProvider.getCurrentUserId()));
    }

    @PostMapping("")
    public ResponseEntity<ItineraryResponse> createItinerary(@Valid @RequestBody CreateItineraryRequest request) {
        ItineraryResponse response = this.itineraryService.createItinerary(this.currentUserProvider.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{itineraryId}")
    public ResponseEntity<ItineraryResponse> getItinerary(@PathVariable Long itineraryId) {
        return ResponseEntity.ok(this.itineraryService.getItinerary(itineraryId, this.currentUserProvider.getCurrentUserId()));
    }

    @GetMapping("/by-group/{groupName}")
    public ResponseEntity<ItineraryResponse> getItineraryByGroupName(@PathVariable String groupName) {
        return ResponseEntity.ok(this.itineraryService.getItineraryByGroupName(groupName, this.currentUserProvider.getCurrentUserId()));
    }

    @PutMapping("/{itineraryId}")
    public ResponseEntity<ItineraryResponse> updateItinerary(
            @PathVariable Long itineraryId,
            @Valid @RequestBody UpdateItineraryRequest request) {
        return ResponseEntity.ok(this.itineraryService.updateItinerary(itineraryId, this.currentUserProvider.getCurrentUserId(), request));
    }

    @DeleteMapping("/{itineraryId}")
    public ResponseEntity<Void> deleteItinerary(@PathVariable Long itineraryId) {
        this.itineraryService.deleteItinerary(itineraryId, this.currentUserProvider.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{itineraryId}/days")
    public ResponseEntity<ItineraryResponse> createDay(
            @PathVariable Long itineraryId,
            @Valid @RequestBody CreateItineraryDayRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(this.itineraryService.createDay(itineraryId, this.currentUserProvider.getCurrentUserId(), request));
    }

    @PutMapping("/{itineraryId}/days/{dayId}")
    public ResponseEntity<ItineraryResponse> updateDay(
            @PathVariable Long itineraryId,
            @PathVariable Long dayId,
            @Valid @RequestBody UpdateItineraryDayRequest request) {
        return ResponseEntity.ok(this.itineraryService.updateDay(itineraryId, dayId, this.currentUserProvider.getCurrentUserId(), request));
    }

    @DeleteMapping("/{itineraryId}/days/{dayId}")
    public ResponseEntity<ItineraryResponse> deleteDay(
            @PathVariable Long itineraryId,
            @PathVariable Long dayId) {
        return ResponseEntity.ok(this.itineraryService.deleteDay(itineraryId, dayId, this.currentUserProvider.getCurrentUserId()));
    }

    @PostMapping("/{itineraryId}/stops")
    public ResponseEntity<ItineraryResponse> createStop(
            @PathVariable Long itineraryId,
            @Valid @RequestBody CreateItineraryStopRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(this.itineraryService.createStop(itineraryId, this.currentUserProvider.getCurrentUserId(), request));
    }

    @PutMapping("/{itineraryId}/stops/{stopId}")
    public ResponseEntity<ItineraryResponse> updateStop(
            @PathVariable Long itineraryId,
            @PathVariable Long stopId,
            @Valid @RequestBody UpdateItineraryStopRequest request) {
        return ResponseEntity.ok(this.itineraryService.updateStop(itineraryId, stopId, this.currentUserProvider.getCurrentUserId(), request));
    }

    @DeleteMapping("/{itineraryId}/stops/{stopId}")
    public ResponseEntity<ItineraryResponse> deleteStop(
            @PathVariable Long itineraryId,
            @PathVariable Long stopId) {
        return ResponseEntity.ok(this.itineraryService.deleteStop(itineraryId, stopId, this.currentUserProvider.getCurrentUserId()));
    }
}
