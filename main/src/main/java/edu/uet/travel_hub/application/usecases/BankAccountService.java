package edu.uet.travel_hub.application.usecases;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.application.dto.request.CreateBankAccountRequest;
import edu.uet.travel_hub.application.dto.response.BankAccountResponse;
import edu.uet.travel_hub.application.exception.ResourceNotFoundException;
import edu.uet.travel_hub.infrastructure.persistence.entity.BankAccountEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.BankAccountJpaRepository;

@Service
public class BankAccountService {
    private final BankAccountJpaRepository bankAccountJpaRepository;
    private final TripService tripService;

    public BankAccountService(BankAccountJpaRepository bankAccountJpaRepository, TripService tripService) {
        this.bankAccountJpaRepository = bankAccountJpaRepository;
        this.tripService = tripService;
    }

    @Transactional(readOnly = true)
    public List<BankAccountResponse> listMyBankAccounts(Long currentUserId) {
        return this.bankAccountJpaRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(currentUserId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BankAccountResponse create(Long currentUserId, CreateBankAccountRequest request) {
        UserEntity user = this.tripService.findUser(currentUserId);
        boolean firstAccount = !this.bankAccountJpaRepository.existsByUserId(currentUserId);
        boolean makeDefault = firstAccount || Boolean.TRUE.equals(request.isDefault());
        if (makeDefault) {
            clearDefault(currentUserId);
        }
        BankAccountEntity saved = this.bankAccountJpaRepository.save(BankAccountEntity.builder()
                .user(user)
                .bankCode(normalize(request.bankCode(), "bankCode"))
                .bankName(normalize(request.bankName(), "bankName"))
                .accountNumber(normalize(request.accountNumber(), "accountNumber"))
                .accountName(normalize(request.accountName(), "accountName"))
                .isDefault(makeDefault)
                .build());
        return toResponse(saved);
    }

    @Transactional
    public BankAccountResponse setDefault(Long currentUserId, Long accountId) {
        BankAccountEntity target = this.bankAccountJpaRepository.findByIdAndUserId(accountId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found"));
        clearDefault(currentUserId);
        target.setIsDefault(true);
        return toResponse(this.bankAccountJpaRepository.save(target));
    }

    private void clearDefault(Long userId) {
        this.bankAccountJpaRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId)
                .forEach(account -> {
                    if (Boolean.TRUE.equals(account.getIsDefault())) {
                        account.setIsDefault(false);
                    }
                });
    }

    private BankAccountResponse toResponse(BankAccountEntity account) {
        return new BankAccountResponse(
                account.getId(),
                account.getBankCode(),
                account.getBankName(),
                account.getAccountNumber(),
                account.getAccountName(),
                account.getIsDefault(),
                account.getCreatedAt(),
                account.getUpdatedAt());
    }

    private String normalize(String value, String fieldName) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized;
    }
}
