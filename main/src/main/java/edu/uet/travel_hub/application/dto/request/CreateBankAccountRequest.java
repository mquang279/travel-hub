package edu.uet.travel_hub.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateBankAccountRequest(
        @NotBlank String bankCode,
        @NotBlank String bankName,
        @NotBlank String accountNumber,
        @NotBlank String accountName,
        Boolean isDefault) {
}
