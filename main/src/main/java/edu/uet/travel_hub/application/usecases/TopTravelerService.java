package edu.uet.travel_hub.application.usecases;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.uet.travel_hub.application.dto.response.PaginationResponse;
import edu.uet.travel_hub.application.dto.response.TopTravelerResponse;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.domain.enums.TopTravelerPeriod;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TopTravelerStatsProjection;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.UserJpaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TopTravelerService {
    private static final ZoneId RANKING_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final UserJpaRepository userJpaRepository;
    private final CurrentUserProvider currentUserProvider;

    @Transactional(readOnly = true)
    public PaginationResponse<TopTravelerResponse> getTopTravelers(
            TopTravelerPeriod period,
            int page,
            int pageSize) {
        Long currentUserId = currentUserProvider.getOptionalCurrentUserId().orElse(null);
        Instant now = Instant.now();
        Instant startAt = periodStart(period, now);
        Page<TopTravelerStatsProjection> ranking = userJpaRepository.findTopTravelers(
                currentUserId,
                startAt,
                now,
                PageRequest.of(page, pageSize));
        List<TopTravelerResponse> data = ranking.getContent().stream()
                .map(item -> new TopTravelerResponse(
                        item.getId(),
                        item.getUsername(),
                        item.getName(),
                        item.getAvatarUrl(),
                        item.getScore() == null ? 0L : item.getScore(),
                        Boolean.TRUE.equals(item.getFollowing()),
                        Boolean.TRUE.equals(item.getCurrentUser())))
                .toList();

        return new PaginationResponse<>(
                ranking.getNumber(),
                ranking.getSize(),
                ranking.getTotalPages(),
                ranking.getTotalElements(),
                data);
    }

    Instant periodStart(TopTravelerPeriod period, Instant now) {
        ZonedDateTime localNow = now.atZone(RANKING_ZONE);
        ZonedDateTime start = switch (period) {
            case WEEK -> localNow
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .toLocalDate()
                    .atStartOfDay(RANKING_ZONE);
            case MONTH -> localNow
                    .withDayOfMonth(1)
                    .toLocalDate()
                    .atStartOfDay(RANKING_ZONE);
        };
        return start.toInstant();
    }
}
