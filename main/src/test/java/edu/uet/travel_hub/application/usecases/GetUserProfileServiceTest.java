package edu.uet.travel_hub.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import edu.uet.travel_hub.application.mapper.UserProfileMapper;
import edu.uet.travel_hub.application.port.out.FollowRepository;
import edu.uet.travel_hub.application.port.out.PostRepository;
import edu.uet.travel_hub.application.port.out.UserRepository;
import edu.uet.travel_hub.domain.dto.response.UserProfileResponse;
import edu.uet.travel_hub.domain.model.UserModel;

class GetUserProfileServiceTest {
    @Test
    void getProfile_usesStoredPostCount() {
        UserRepository userRepository = mock(UserRepository.class);
        FollowRepository followRepository = mock(FollowRepository.class);
        PostRepository postRepository = mock(PostRepository.class);
        GetUserProfileService service = new GetUserProfileService(
                userRepository,
                followRepository,
                new UserProfileMapper());
        UserModel user = UserModel.builder()
                .id(1L)
                .username("traveler")
                .postsCount(3)
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse response = service.getProfile(1L, 1L);

        assertThat(response.getPostsCount()).isEqualTo(3);
        verify(postRepository, never()).countByUserId(1L);
    }
}
