package edu.uet.travel_hub.interfaces.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.uet.travel_hub.application.dto.response.PaginationResponse;
import edu.uet.travel_hub.application.dto.response.TravelPlaceViewHistoryResponse;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.usecases.TravelPlaceService;

@RestController
@RequestMapping("/api/users/me/place-view-history")
public class TravelPlaceHistoryController {
    private final TravelPlaceService travelPlaceService;
    private final CurrentUserProvider currentUserProvider;

    public TravelPlaceHistoryController(TravelPlaceService travelPlaceService, CurrentUserProvider currentUserProvider) {
        this.travelPlaceService = travelPlaceService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("")
    public ResponseEntity<PaginationResponse<TravelPlaceViewHistoryResponse>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(
                this.travelPlaceService.getViewHistory(this.currentUserProvider.getCurrentUserId(), page, pageSize));
    }
}
