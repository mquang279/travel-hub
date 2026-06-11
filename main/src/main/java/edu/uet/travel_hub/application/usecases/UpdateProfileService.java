package edu.uet.travel_hub.application.usecases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.application.exception.ResourceNotFoundException;
import edu.uet.travel_hub.application.mapper.UserProfileMapper;
import edu.uet.travel_hub.application.port.in.UpdateProfileUseCase;
import edu.uet.travel_hub.application.port.out.UserRepository;
import edu.uet.travel_hub.domain.dto.request.UpdateProfileRequest;
import edu.uet.travel_hub.domain.dto.response.UserProfileResponse;
import edu.uet.travel_hub.domain.model.UserModel;
import edu.uet.travel_hub.infrastructure.persistence.entity.BankAccountEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.BankAccountJpaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateProfileService implements UpdateProfileUseCase {
    private final UserRepository userRepository;
    private final UserProfileMapper userProfileMapper;
    private final BankAccountJpaRepository bankAccountJpaRepository;

    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user.setUsername(request.getUsername());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getLocation() != null) {
            user.setLocation(request.getLocation());
        }

        UserModel savedUser = userRepository.save(user);
        UserProfileResponse response = userProfileMapper.toProfileResponse(savedUser, false);
        enrichDefaultBankAccount(response, userId);
        return response;
    }

    private void enrichDefaultBankAccount(UserProfileResponse response, Long userId) {
        BankAccountEntity bankAccount = bankAccountJpaRepository.findFirstByUserIdAndIsDefaultTrue(userId)
                .orElse(null);
        response.setHasBankAccount(bankAccount != null);
        if (bankAccount == null) {
            return;
        }
        response.setBankCode(bankAccount.getBankCode());
        response.setBankName(bankAccount.getBankName());
        response.setAccountNumber(bankAccount.getAccountNumber());
        response.setAccountName(bankAccount.getAccountName());
    }
}
