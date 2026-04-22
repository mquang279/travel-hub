package edu.uet.travel_hub.interfaces.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.uet.travel_hub.application.dto.request.UpsertTravelPlaceReviewRequest;
import edu.uet.travel_hub.application.dto.response.PaginationResponse;
import edu.uet.travel_hub.application.dto.response.TravelPlaceDetailResponse;
import edu.uet.travel_hub.application.dto.response.TravelPlaceListItemResponse;
import edu.uet.travel_hub.application.dto.response.TravelPlaceReviewResponse;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.usecases.TravelPlaceService;
import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/places")
public class TravelPlaceController {
    private final TravelPlaceService travelPlaceService;
    private final CurrentUserProvider currentUserProvider;

    public TravelPlaceController(TravelPlaceService travelPlaceService, CurrentUserProvider currentUserProvider) {
        this.travelPlaceService = travelPlaceService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("")
    public ResponseEntity<PaginationResponse<TravelPlaceListItemResponse>> getPlaces(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long provinceId,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(this.travelPlaceService.getPlaces(page, pageSize, provinceId, keyword));
    }

    @GetMapping("/{placeId}")
    public ResponseEntity<TravelPlaceDetailResponse> getPlaceDetail(@PathVariable Long placeId) {
        return ResponseEntity.ok(this.travelPlaceService.getPlaceDetail(placeId, this.currentUserProvider.getOptionalCurrentUserId()));
    }

    @GetMapping("/{placeId}/reviews")
    public ResponseEntity<PaginationResponse<TravelPlaceReviewResponse>> getReviews(
            @PathVariable Long placeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(this.travelPlaceService.getReviews(placeId, page, pageSize));
    }

    @PutMapping("/{placeId}/review")
    public ResponseEntity<TravelPlaceReviewResponse> upsertReview(
            @PathVariable Long placeId,
            @Valid @RequestBody UpsertTravelPlaceReviewRequest request) {
        TravelPlaceReviewResponse response = this.travelPlaceService.upsertReview(
                placeId,
                this.currentUserProvider.getCurrentUserId(),
                request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
