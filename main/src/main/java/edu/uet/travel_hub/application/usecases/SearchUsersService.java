package edu.uet.travel_hub.application.usecases;

import org.springframework.stereotype.Service;

import edu.uet.travel_hub.application.dto.response.PaginationResponse;
import edu.uet.travel_hub.application.mapper.UserProfileMapper;
import edu.uet.travel_hub.application.port.in.SearchUsersUseCase;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.port.out.FollowRepository;
import edu.uet.travel_hub.application.port.out.UserRepository;
import edu.uet.travel_hub.domain.dto.response.UserProfileResponse;
import edu.uet.travel_hub.domain.model.UserModel;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchUsersService implements SearchUsersUseCase {
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final CurrentUserProvider currentUserProvider;
    private final UserProfileMapper userProfileMapper;

    @Override
    public PaginationResponse<UserProfileResponse> searchByUsername(String username, int pageNumber, int pageSize) {
        PaginationResponse<UserModel> users = this.userRepository.searchByUsername(username, pageNumber, pageSize);
        Long currentUserId = this.currentUserProvider.getOptionalCurrentUserId().orElse(null);

        return new PaginationResponse<>(
                users.pageNumber(),
                users.pageSize(),
                users.totalPages(),
                users.totalElements(),
                users.data().stream()
                        .map(user -> this.userProfileMapper.toProfileResponse(user, isFollowing(currentUserId, user)))
                        .toList());
    }

    private boolean isFollowing(Long currentUserId, UserModel user) {
        return currentUserId != null
                && !currentUserId.equals(user.getId())
                && this.followRepository.existsByFollowerIdAndFollowingId(currentUserId, user.getId());
    }
}
