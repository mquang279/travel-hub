package edu.uet.travel_hub.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import edu.uet.travel_hub.application.exception.InviteCodeGenerationException;
import edu.uet.travel_hub.infrastructure.persistence.entity.TripEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TripJpaRepository;

public class InviteCodeServiceTest {

    @Mock
    private TripJpaRepository tripJpaRepository;

    private InviteCodeService inviteCodeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        inviteCodeService = new InviteCodeService(tripJpaRepository);
    }

    @Test
    void generateInviteCode_returns8CharUpperAlnum() {
        when(tripJpaRepository.findByInviteCode(anyString())).thenReturn(Optional.empty());
        String code = inviteCodeService.generateInviteCode();
        assertThat(code).isNotNull();
        assertThat(code.length()).isEqualTo(8);
        assertThat(code).matches("^[A-Z0-9]{8}$");
    }

    @Test
    void generateInviteCode_retriesOnCollision_andSucceeds() {
        when(tripJpaRepository.findByInviteCode(anyString()))
            .thenReturn(Optional.of(mock(TripEntity.class)))
                .thenReturn(Optional.empty());

        String code = inviteCodeService.generateInviteCode();
        assertThat(code).isNotNull();
        assertThat(code.length()).isEqualTo(8);
    }

    @Test
    void generateInviteCode_throwsAfter5Retries() {
        when(tripJpaRepository.findByInviteCode(anyString())).thenReturn(Optional.of(mock(TripEntity.class)));
        assertThatThrownBy(() -> inviteCodeService.generateInviteCode())
                .isInstanceOf(InviteCodeGenerationException.class);
    }
}
