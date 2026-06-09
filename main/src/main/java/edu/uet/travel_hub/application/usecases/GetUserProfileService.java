package edu.uet.travel_hub.application.usecases;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import edu.uet.travel_hub.application.exception.ResourceNotFoundException;
import edu.uet.travel_hub.application.mapper.UserProfileMapper;
import edu.uet.travel_hub.application.port.in.GetUserProfileUseCase;
import edu.uet.travel_hub.application.port.out.FollowRepository;
import edu.uet.travel_hub.application.port.out.UserRepository;
import edu.uet.travel_hub.domain.dto.response.UserProfileResponse;
import edu.uet.travel_hub.domain.model.UserModel;
import edu.uet.travel_hub.infrastructure.persistence.entity.BankAccountEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.BankAccountJpaRepository;

@Service
public class GetUserProfileService implements GetUserProfileUseCase {
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final UserProfileMapper userProfileMapper;
    private final BankAccountJpaRepository bankAccountJpaRepository;

    @Autowired
    public GetUserProfileService(
            UserRepository userRepository,
            FollowRepository followRepository,
            UserProfileMapper userProfileMapper,
            BankAccountJpaRepository bankAccountJpaRepository) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.userProfileMapper = userProfileMapper;
        this.bankAccountJpaRepository = bankAccountJpaRepository;
    }

    public GetUserProfileService(
            UserRepository userRepository,
            FollowRepository followRepository,
            UserProfileMapper userProfileMapper) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.userProfileMapper = userProfileMapper;
        this.bankAccountJpaRepository = null;
    }

    @Override
    public UserProfileResponse getProfile(Long currentUserId, Long targetUserId) {
        UserModel user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isFollowing = currentUserId != null
                && !currentUserId.equals(targetUserId)
                && followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId);

        UserProfileResponse response = userProfileMapper.toProfileResponse(user, isFollowing);
        enrichDefaultBankAccount(response, targetUserId);
        return response;
    }

    private void enrichDefaultBankAccount(UserProfileResponse response, Long userId) {
        if (bankAccountJpaRepository == null) {
            return;
        }
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
