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

import edu.uet.travel_hub.application.dto.request.CreateTripExpenseRequest;
import edu.uet.travel_hub.application.dto.response.TripExpenseResponse;
import edu.uet.travel_hub.application.dto.response.TripExpenseTransactionResponse;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.usecases.TripExpenseService;
import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/trips/{tripId}/expenses")
public class TripExpenseController {
    private final TripExpenseService tripExpenseService;
    private final CurrentUserProvider currentUserProvider;

    public TripExpenseController(TripExpenseService tripExpenseService, CurrentUserProvider currentUserProvider) {
        this.tripExpenseService = tripExpenseService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("")
    public ResponseEntity<TripExpenseResponse> listExpenses(@PathVariable Long tripId) {
        return ResponseEntity.ok(this.tripExpenseService.listExpenses(tripId, this.currentUserProvider.getCurrentUserId()));
    }

    @PostMapping("")
    public ResponseEntity<TripExpenseTransactionResponse> addExpense(
            @PathVariable Long tripId,
            @Valid @RequestBody CreateTripExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(this.tripExpenseService.addExpense(tripId, this.currentUserProvider.getCurrentUserId(), request));
    }
}