package edu.uet.travel_hub.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.uet.travel_hub.application.dto.response.SavePostResponse;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.port.out.PostRepository;
import edu.uet.travel_hub.application.port.out.SavedPostRepository;
import edu.uet.travel_hub.domain.model.PostModel;

class SavePostServiceTest {
    private SavedPostRepository savedPostRepository;
    private PostRepository postRepository;
    private CurrentUserProvider currentUserProvider;
    private SavePostService savePostService;

    @BeforeEach
    void setUp() {
        savedPostRepository = mock(SavedPostRepository.class);
        postRepository = mock(PostRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        savePostService = new SavePostService(savedPostRepository, postRepository, currentUserProvider);
    }

    @Test
    void save_createsSavedPostWhenMissing() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(postRepository.findById(10L)).thenReturn(Optional.of(PostModel.builder().id(10L).build()));
        when(savedPostRepository.exists(1L, 10L)).thenReturn(false);

        SavePostResponse response = savePostService.save(10L);

        assertThat(response.saved()).isTrue();
        verify(savedPostRepository).save(1L, 10L);
    }

    @Test
    void save_togglesOffExistingSavedPost() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(postRepository.findById(10L)).thenReturn(Optional.of(PostModel.builder().id(10L).build()));
        when(savedPostRepository.exists(1L, 10L)).thenReturn(true);

        SavePostResponse response = savePostService.save(10L);

        assertThat(response.saved()).isFalse();
        verify(savedPostRepository, never()).save(1L, 10L);
        verify(savedPostRepository).delete(1L, 10L);
    }

    @Test
    void unsave_deletesSavedPost() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(postRepository.findById(10L)).thenReturn(Optional.of(PostModel.builder().id(10L).build()));

        SavePostResponse response = savePostService.unsave(10L);

        assertThat(response.saved()).isFalse();
        verify(savedPostRepository).delete(1L, 10L);
    }
}
