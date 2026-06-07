package edu.uet.travel_hub.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;

import edu.uet.travel_hub.application.dto.response.TravelPlaceListItemResponse;
import edu.uet.travel_hub.infrastructure.persistence.entity.ProvinceEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.TravelPlaceEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.TravelPlaceImageEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.ProvinceJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TravelPlaceImageJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TravelPlaceJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TravelPlaceReviewJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TravelPlaceReviewStatsProjection;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TravelPlaceViewHistoryJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.UserJpaRepository;

class TravelPlaceServiceTest {
    private TravelPlaceJpaRepository travelPlaceJpaRepository;
    private TravelPlaceImageJpaRepository travelPlaceImageJpaRepository;
    private TravelPlaceReviewJpaRepository travelPlaceReviewJpaRepository;
    private TravelPlaceService travelPlaceService;

    @BeforeEach
    void setUp() {
        travelPlaceJpaRepository = mock(TravelPlaceJpaRepository.class);
        travelPlaceImageJpaRepository = mock(TravelPlaceImageJpaRepository.class);
        travelPlaceReviewJpaRepository = mock(TravelPlaceReviewJpaRepository.class);
        travelPlaceService = new TravelPlaceService(
                mock(ProvinceJpaRepository.class),
                travelPlaceJpaRepository,
                travelPlaceImageJpaRepository,
                travelPlaceReviewJpaRepository,
                mock(TravelPlaceViewHistoryJpaRepository.class),
                mock(UserJpaRepository.class),
                mock(PlatformTransactionManager.class));
    }

    @Test
    void getFeaturedPlacesRequestsFiveItemsAndKeepsRankingOrderAfterHydration() {
        ProvinceEntity province = ProvinceEntity.builder().id(1L).name("Ha Noi").build();
        TravelPlaceEntity first = TravelPlaceEntity.builder().id(10L).name("First").province(province).build();
        TravelPlaceEntity second = TravelPlaceEntity.builder().id(20L).name("Second").province(province).build();
        TravelPlaceImageEntity firstImage = TravelPlaceImageEntity.builder()
                .place(first)
                .imageUrl("first.jpg")
                .main(true)
                .build();
        TravelPlaceReviewStatsProjection firstStats = reviewStats(10L, 4.8, 12L);
        TravelPlaceReviewStatsProjection secondStats = reviewStats(20L, 4.5, 8L);

        when(travelPlaceJpaRepository.findFeaturedPlaceIds(PageRequest.of(0, 5)))
                .thenReturn(List.of(20L, 10L));
        when(travelPlaceJpaRepository.findByIdIn(List.of(20L, 10L)))
                .thenReturn(List.of(first, second));
        when(travelPlaceImageJpaRepository.findByPlaceIdInOrderByPlaceIdAscMainDescIdAsc(List.of(20L, 10L)))
                .thenReturn(List.of(firstImage));
        when(travelPlaceReviewJpaRepository.getStatsByPlaceIds(List.of(20L, 10L)))
                .thenReturn(List.of(firstStats, secondStats));

        List<TravelPlaceListItemResponse> result = travelPlaceService.getFeaturedPlaces();

        assertThat(result).extracting(TravelPlaceListItemResponse::id).containsExactly(20L, 10L);
        assertThat(result.get(0).averageRating()).isEqualTo(4.5);
        assertThat(result.get(1).mainImage()).isEqualTo("first.jpg");
        assertThat(result.get(1).images()).extracting("imageUrl").containsExactly("first.jpg");
        verify(travelPlaceJpaRepository).findFeaturedPlaceIds(PageRequest.of(0, 5));
    }

    private TravelPlaceReviewStatsProjection reviewStats(Long placeId, Double averageRating, Long reviewCount) {
        TravelPlaceReviewStatsProjection stats = mock(TravelPlaceReviewStatsProjection.class);
        when(stats.getPlaceId()).thenReturn(placeId);
        when(stats.getAverageRating()).thenReturn(averageRating);
        when(stats.getReviewCount()).thenReturn(reviewCount);
        return stats;
    }
}
