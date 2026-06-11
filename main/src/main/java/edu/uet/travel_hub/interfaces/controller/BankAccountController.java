package edu.uet.travel_hub.interfaces.controller;

import java.util.List;

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

import edu.uet.travel_hub.application.dto.request.CreateBankAccountRequest;
import edu.uet.travel_hub.application.dto.response.BankAccountResponse;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.usecases.BankAccountService;
import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/me/bank-accounts")
public class BankAccountController {
    private final BankAccountService bankAccountService;
    private final CurrentUserProvider currentUserProvider;

    public BankAccountController(BankAccountService bankAccountService, CurrentUserProvider currentUserProvider) {
        this.bankAccountService = bankAccountService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("")
    public ResponseEntity<BankAccountResponse> create(@Valid @RequestBody CreateBankAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(this.bankAccountService.create(this.currentUserProvider.getCurrentUserId(), request));
    }

    @PutMapping("/default")
    public ResponseEntity<BankAccountResponse> upsertDefault(@Valid @RequestBody CreateBankAccountRequest request) {
        return ResponseEntity.ok(this.bankAccountService.upsertDefault(this.currentUserProvider.getCurrentUserId(), request));
    }

    @GetMapping("")
    public ResponseEntity<List<BankAccountResponse>> listMine() {
        return ResponseEntity.ok(this.bankAccountService.listMyBankAccounts(this.currentUserProvider.getCurrentUserId()));
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<BankAccountResponse> setDefault(@PathVariable Long id) {
        return ResponseEntity.ok(this.bankAccountService.setDefault(this.currentUserProvider.getCurrentUserId(), id));
    }
}
