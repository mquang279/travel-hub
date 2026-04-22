package edu.uet.travel_hub.interfaces.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.uet.travel_hub.application.dto.request.UpsertTravelPlaceRequest;
import edu.uet.travel_hub.application.dto.response.TravelPlaceDetailResponse;
import edu.uet.travel_hub.application.usecases.TravelPlaceService;
import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/admin/places")
public class AdminTravelPlaceController {
    private final TravelPlaceService travelPlaceService;

    public AdminTravelPlaceController(TravelPlaceService travelPlaceService) {
        this.travelPlaceService = travelPlaceService;
    }

    @GetMapping("/{placeId}")
    public ResponseEntity<TravelPlaceDetailResponse> getPlaceDetailForAdmin(@PathVariable Long placeId) {
        return ResponseEntity.ok(this.travelPlaceService.getPlaceDetailForAdmin(placeId));
    }

    @PostMapping("")
    public ResponseEntity<TravelPlaceDetailResponse> createPlace(@Valid @RequestBody UpsertTravelPlaceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.travelPlaceService.createPlace(request));
    }

    @PutMapping("/{placeId}")
    public ResponseEntity<TravelPlaceDetailResponse> updatePlace(
            @PathVariable Long placeId,
            @Valid @RequestBody UpsertTravelPlaceRequest request) {
        return ResponseEntity.ok(this.travelPlaceService.updatePlace(placeId, request));
    }
}
