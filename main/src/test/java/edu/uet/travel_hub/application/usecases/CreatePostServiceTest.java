package edu.uet.travel_hub.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.uet.travel_hub.application.dto.request.CreatePostRequest;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.port.out.PostRepository;
import edu.uet.travel_hub.application.port.out.UserRepository;
import edu.uet.travel_hub.domain.model.PostModel;

class CreatePostServiceTest {
    private CurrentUserProvider currentUserProvider;
    private PostRepository postRepository;
    private UserRepository userRepository;
    private CreatePostService createPostService;

    @BeforeEach
    void setUp() {
        currentUserProvider = mock(CurrentUserProvider.class);
        postRepository = mock(PostRepository.class);
        userRepository = mock(UserRepository.class);
        createPostService = new CreatePostService(
                currentUserProvider,
                postRepository,
                userRepository);
    }

    @Test
    void create_incrementsPostCountForCurrentUser() {
        CreatePostRequest request = new CreatePostRequest("New post", java.util.List.of("image.jpg"), null);
        PostModel savedPost = PostModel.builder()
                .id(10L)
                .userId(1L)
                .description(request.description())
                .imageUrls(request.imageUrls())
                .build();
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(postRepository.save(eq(1L), any(PostModel.class))).thenReturn(savedPost);

        PostModel result = createPostService.create(request);

        assertThat(result).isSameAs(savedPost);
        verify(userRepository).incrementPosts(1L);
    }
}
