package edu.uet.travel_hub.interfaces.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import edu.uet.travel_hub.application.dto.response.PaymentProofResponse;
import edu.uet.travel_hub.application.dto.response.SettlementResponse;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.usecases.SettlementService;

@RestController
@Validated
@RequestMapping("/api")
public class SettlementController {
    private final SettlementService settlementService;
    private final CurrentUserProvider currentUserProvider;

    public SettlementController(SettlementService settlementService, CurrentUserProvider currentUserProvider) {
        this.settlementService = settlementService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/trips/{tripId}/settlements")
    public ResponseEntity<List<SettlementResponse>> listForTrip(@PathVariable Long tripId) {
        return ResponseEntity.ok(this.settlementService.listSettlements(tripId, this.currentUserProvider.getCurrentUserId()));
    }

    @GetMapping("/settlements/{settlementId}")
    public ResponseEntity<SettlementResponse> get(@PathVariable Long settlementId) {
        return ResponseEntity.ok(this.settlementService.getSettlement(settlementId, this.currentUserProvider.getCurrentUserId()));
    }

    @PostMapping(value = "/settlements/{settlementId}/proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PaymentProofResponse> uploadProof(
            @PathVariable Long settlementId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "note", required = false) String note) {
        return ResponseEntity.ok(this.settlementService.uploadProof(
                settlementId,
                this.currentUserProvider.getCurrentUserId(),
                file,
                note));
    }

    @PostMapping("/settlements/{settlementId}/confirm")
    public ResponseEntity<SettlementResponse> confirm(@PathVariable Long settlementId) {
        return ResponseEntity.ok(this.settlementService.confirm(settlementId, this.currentUserProvider.getCurrentUserId()));
    }

    @PostMapping("/settlements/{settlementId}/reject")
    public ResponseEntity<SettlementResponse> reject(@PathVariable Long settlementId) {
        return ResponseEntity.ok(this.settlementService.reject(settlementId, this.currentUserProvider.getCurrentUserId()));
    }
}
