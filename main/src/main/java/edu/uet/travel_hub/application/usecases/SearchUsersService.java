package edu.uet.travel_hub.application.usecases;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        Set<Long> followingIds = findFollowingIds(currentUserId, users.data());

        return new PaginationResponse<>(
                users.pageNumber(),
                users.pageSize(),
                users.totalPages(),
                users.totalElements(),
                users.data().stream()
                        .map(user -> this.userProfileMapper.toProfileResponse(user, followingIds.contains(user.getId())))
                        .toList());
    }

    private Set<Long> findFollowingIds(Long currentUserId, List<UserModel> users) {
        if (currentUserId == null || users.isEmpty()) {
            return Set.of();
        }
        Set<Long> userIds = users.stream()
                .map(UserModel::getId)
                .filter(id -> !currentUserId.equals(id))
                .collect(Collectors.toSet());
        return this.followRepository.findFollowingIds(currentUserId, userIds);
    }
}
