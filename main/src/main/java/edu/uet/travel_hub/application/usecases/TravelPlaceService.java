package edu.uet.travel_hub.application.usecases;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityNotFoundException;
import edu.uet.travel_hub.application.dto.request.UpsertTravelPlaceRequest;
import edu.uet.travel_hub.application.dto.request.UpsertTravelPlaceReviewRequest;
import edu.uet.travel_hub.application.dto.response.PaginationResponse;
import edu.uet.travel_hub.application.dto.response.ProvinceResponse;
import edu.uet.travel_hub.application.dto.response.TravelPlaceDetailResponse;
import edu.uet.travel_hub.application.dto.response.TravelPlaceImageResponse;
import edu.uet.travel_hub.application.dto.response.TravelPlaceListItemResponse;
import edu.uet.travel_hub.application.dto.response.TravelPlaceReviewAuthorResponse;
import edu.uet.travel_hub.application.dto.response.TravelPlaceReviewResponse;
import edu.uet.travel_hub.application.dto.response.TravelPlaceReviewSummaryResponse;
import edu.uet.travel_hub.application.dto.response.TravelPlaceViewHistoryResponse;
import edu.uet.travel_hub.application.exception.ResourceNotFoundException;
import edu.uet.travel_hub.infrastructure.persistence.entity.ProvinceEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.TravelPlaceEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.TravelPlaceImageEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.TravelPlaceReviewEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.TravelPlaceViewHistoryEntity;
import edu.uet.travel_hub.infrastructure.persistence.entity.UserEntity;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.ProvinceJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TravelPlaceImageJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TravelPlaceJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TravelPlaceReviewJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TravelPlaceReviewStatsProjection;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.TravelPlaceViewHistoryJpaRepository;
import edu.uet.travel_hub.infrastructure.persistence.repository.jpa.UserJpaRepository;

@Service
public class TravelPlaceService {
    private static final int FEATURED_PLACE_LIMIT = 5;
    private static final Logger log = LoggerFactory.getLogger(TravelPlaceService.class);
    private static final TravelPlaceReviewAuthorResponse UNKNOWN_REVIEW_AUTHOR = new TravelPlaceReviewAuthorResponse(
            -1L, "Người dùng Travel Hub", "travelhub_user", null);

    private final ProvinceJpaRepository provinceJpaRepository;
    private final TravelPlaceJpaRepository travelPlaceJpaRepository;
    private final TravelPlaceImageJpaRepository travelPlaceImageJpaRepository;
    private final TravelPlaceReviewJpaRepository travelPlaceReviewJpaRepository;
    private final TravelPlaceViewHistoryJpaRepository travelPlaceViewHistoryJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final TransactionTemplate sideEffectTransactionTemplate;

    public TravelPlaceService(
            ProvinceJpaRepository provinceJpaRepository,
            TravelPlaceJpaRepository travelPlaceJpaRepository,
            TravelPlaceImageJpaRepository travelPlaceImageJpaRepository,
            TravelPlaceReviewJpaRepository travelPlaceReviewJpaRepository,
            TravelPlaceViewHistoryJpaRepository travelPlaceViewHistoryJpaRepository,
            UserJpaRepository userJpaRepository,
            PlatformTransactionManager transactionManager) {
        this.provinceJpaRepository = provinceJpaRepository;
        this.travelPlaceJpaRepository = travelPlaceJpaRepository;
        this.travelPlaceImageJpaRepository = travelPlaceImageJpaRepository;
        this.travelPlaceReviewJpaRepository = travelPlaceReviewJpaRepository;
        this.travelPlaceViewHistoryJpaRepository = travelPlaceViewHistoryJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.sideEffectTransactionTemplate = new TransactionTemplate(transactionManager);
        this.sideEffectTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional(readOnly = true)
    public PaginationResponse<TravelPlaceListItemResponse> getPlaces(int page, int pageSize, Long provinceId,
            String keyword) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<TravelPlaceEntity> places = this.travelPlaceJpaRepository.search(provinceId, normalizeKeyword(keyword),
                pageable);

        List<Long> placeIds = places.stream().map(TravelPlaceEntity::getId).toList();
        Map<Long, List<TravelPlaceImageResponse>> imagesByPlaceId = resolveImagesByPlaceId(placeIds);
        Map<Long, TravelPlaceReviewSummaryResponse> reviewSummaries = resolveReviewSummaryByPlaceId(placeIds);
        List<TravelPlaceListItemResponse> data = places.getContent().stream()
                .map(place -> toListItemResponse(
                        place,
                        imagesByPlaceId.getOrDefault(place.getId(), List.of()),
                        reviewSummaries.getOrDefault(place.getId(), emptyReviewSummary())))
                .toList();

        return new PaginationResponse<>(places.getNumber(), places.getSize(), places.getTotalPages(),
                places.getTotalElements(), data);
    }

    @Transactional(readOnly = true)
    public List<TravelPlaceListItemResponse> getFeaturedPlaces() {
        List<Long> placeIds = this.travelPlaceJpaRepository.findFeaturedPlaceIds(
                PageRequest.of(0, FEATURED_PLACE_LIMIT));
        Map<Long, TravelPlaceEntity> placesById = this.travelPlaceJpaRepository.findByIdIn(placeIds).stream()
                .collect(LinkedHashMap::new, (places, place) -> places.put(place.getId(), place), Map::putAll);
        Map<Long, List<TravelPlaceImageResponse>> imagesByPlaceId = resolveImagesByPlaceId(placeIds);
        Map<Long, TravelPlaceReviewSummaryResponse> reviewSummaries = resolveReviewSummaryByPlaceId(placeIds);

        return placeIds.stream()
                .map(placesById::get)
                .filter(Objects::nonNull)
                .map(place -> toListItemResponse(
                        place,
                        imagesByPlaceId.getOrDefault(place.getId(), List.of()),
                        reviewSummaries.getOrDefault(place.getId(), emptyReviewSummary())))
                .toList();
    }

    @Transactional(readOnly = true)
    public TravelPlaceDetailResponse getPlaceDetail(Long placeId, Optional<Long> currentUserId) {
        TravelPlaceEntity place = findPlaceById(placeId);
        boolean viewIncremented = incrementViewsSafely(placeId);
        currentUserId.ifPresent(userId -> recordViewHistorySafely(placeId, userId));

        Integer displayedViews = (place.getViews() == null ? 0 : place.getViews())
                + (viewIncremented ? 1 : 0);
        return buildPlaceDetailResponse(place, currentUserId, displayedViews);
    }

    @Transactional(readOnly = true)
    public TravelPlaceDetailResponse getPlaceDetailForAdmin(Long placeId) {
        return buildPlaceDetailResponse(findPlaceById(placeId), Optional.empty());
    }

    @Transactional
    public TravelPlaceDetailResponse createPlace(UpsertTravelPlaceRequest request) {
        ProvinceEntity province = this.provinceJpaRepository.findById(request.provinceId())
                .orElseThrow(() -> new ResourceNotFoundException("Province not found"));

        TravelPlaceEntity place = TravelPlaceEntity.builder()
                .province(province)
                .name(request.name().trim())
                .description(request.description())
                .lat(request.lat())
                .lon(request.lon())
                .openingTime(request.openingTime())
                .views(0)
                .build();

        TravelPlaceEntity saved = this.travelPlaceJpaRepository.save(place);
        replaceImages(saved, request.imageUrls());
        return buildPlaceDetailResponse(saved, Optional.empty());
    }

    @Transactional
    public TravelPlaceDetailResponse updatePlace(Long placeId, UpsertTravelPlaceRequest request) {
        TravelPlaceEntity place = this.travelPlaceJpaRepository.findById(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Travel place not found"));
        ProvinceEntity province = this.provinceJpaRepository.findById(request.provinceId())
                .orElseThrow(() -> new ResourceNotFoundException("Province not found"));

        place.setProvince(province);
        place.setName(request.name().trim());
        place.setDescription(request.description());
        place.setLat(request.lat());
        place.setLon(request.lon());
        place.setOpeningTime(request.openingTime());
        this.travelPlaceJpaRepository.save(place);

        replaceImages(place, request.imageUrls());
        return buildPlaceDetailResponse(place, Optional.empty());
    }

    @Transactional(readOnly = true)
    public PaginationResponse<TravelPlaceReviewResponse> getReviews(Long placeId, int page, int pageSize) {
        ensurePlaceExists(placeId);

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "updatedAt", "id"));
        Page<TravelPlaceReviewEntity> reviews = this.travelPlaceReviewJpaRepository
                .findByPlaceIdOrderByUpdatedAtDescIdDesc(placeId, pageable);

        return new PaginationResponse<>(reviews.getNumber(), reviews.getSize(), reviews.getTotalPages(),
                reviews.getTotalElements(), reviews.getContent().stream().map(this::toReviewResponse).toList());
    }

    @Transactional
    public TravelPlaceReviewResponse upsertReview(Long placeId, Long currentUserId,
            UpsertTravelPlaceReviewRequest request) {
        TravelPlaceEntity place = this.travelPlaceJpaRepository.findById(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Travel place not found"));
        UserEntity user = this.userJpaRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TravelPlaceReviewEntity review = this.travelPlaceReviewJpaRepository
                .findByPlaceIdAndUserId(placeId, currentUserId)
                .orElseGet(() -> TravelPlaceReviewEntity.builder()
                        .place(place)
                        .user(user)
                        .build());

        review.setRating(request.rating());
        review.setContent(request.content().trim());

        TravelPlaceReviewEntity saved = this.travelPlaceReviewJpaRepository.save(review);
        return toReviewResponse(saved);
    }

    @Transactional(readOnly = true)
    public PaginationResponse<TravelPlaceViewHistoryResponse> getViewHistory(Long currentUserId, int page,
            int pageSize) {
        int safePage = Math.max(0, page);
        int safePageSize = Math.max(1, pageSize);
        List<TravelPlaceViewHistoryEntity> collapsedHistory = collapseConsecutiveViewHistory(
                this.travelPlaceViewHistoryJpaRepository.findByUserIdOrderByViewedAtDescIdDesc(currentUserId));
        int totalElements = collapsedHistory.size();
        int totalPages = (int) Math.ceil((double) totalElements / safePageSize);
        int fromIndex = Math.min(safePage * safePageSize, totalElements);
        int toIndex = Math.min(fromIndex + safePageSize, totalElements);
        List<TravelPlaceViewHistoryEntity> pageContent = collapsedHistory.subList(fromIndex, toIndex);

        Map<Long, List<TravelPlaceImageResponse>> imagesByPlaceId = resolveImagesByPlaceId(
                pageContent.stream().map(item -> item.getPlace().getId()).toList());

        List<TravelPlaceViewHistoryResponse> data = pageContent.stream()
                .map(item -> new TravelPlaceViewHistoryResponse(
                        item.getPlace().getId(),
                        item.getPlace().getName(),
                        imagesByPlaceId.get(item.getPlace().getId()).get(0).imageUrl(),
                        item.getPlace().getProvince().getName(),
                        item.getViewedAt()))
                .toList();

        return new PaginationResponse<TravelPlaceViewHistoryResponse>(
                safePage,
                safePageSize,
                totalPages,
                (long) totalElements,
                data);
    }

    @Transactional(readOnly = true)
    public PaginationResponse<TravelPlaceListItemResponse> getRecommendedPlaces(
            int page,
            int pageSize,
            Long provinceId) {
        int safePage = Math.max(page, 0);
        int safePageSize = Math.max(pageSize, 1);

        Pageable pageable = PageRequest.of(safePage, safePageSize);
        Page<TravelPlaceEntity> places = this.travelPlaceJpaRepository.findRandom(provinceId, pageable);
        List<Long> placeIds = places.stream().map(TravelPlaceEntity::getId).toList();
        Map<Long, List<TravelPlaceImageResponse>> imagesByPlaceId = resolveImagesByPlaceId(placeIds);
        Map<Long, TravelPlaceReviewSummaryResponse> reviewSummaries = resolveReviewSummaryByPlaceId(placeIds);

        List<TravelPlaceListItemResponse> data = places.getContent().stream()
                .map(place -> toListItemResponse(
                        place,
                        imagesByPlaceId.getOrDefault(place.getId(), List.of()),
                        reviewSummaries.getOrDefault(place.getId(), emptyReviewSummary())))
                .toList();

        return new PaginationResponse<>(places.getNumber(), places.getSize(), places.getTotalPages(),
                places.getTotalElements(), data);
    }

    private TravelPlaceDetailResponse buildPlaceDetailResponse(TravelPlaceEntity place, Optional<Long> currentUserId) {
        return buildPlaceDetailResponse(place, currentUserId, place.getViews());
    }

    private TravelPlaceDetailResponse buildPlaceDetailResponse(
            TravelPlaceEntity place,
            Optional<Long> currentUserId,
            Integer displayedViews) {
        List<TravelPlaceImageResponse> images = this.travelPlaceImageJpaRepository
                .findByPlaceIdOrderByMainDescIdAsc(place.getId())
                .stream()
                .map(image -> new TravelPlaceImageResponse(image.getId(), image.getImageUrl(), image.isMain()))
                .toList();

        TravelPlaceReviewSummaryResponse reviewSummary = getReviewSummary(place.getId());
        TravelPlaceReviewResponse myReview = resolveMyReview(place.getId(), currentUserId);

        return new TravelPlaceDetailResponse(
                place.getId(),
                place.getName(),
                place.getDescription(),
                place.getLat(),
                place.getLon(),
                displayedViews,
                place.getOpeningTime(),
                toProvinceResponse(place.getProvince()),
                images,
                reviewSummary,
                myReview);
    }

    private void replaceImages(TravelPlaceEntity place, List<String> imageUrls) {
        this.travelPlaceImageJpaRepository.deleteByPlaceId(place.getId());

        List<String> sanitizedImageUrls = sanitizeImageUrls(imageUrls);
        List<TravelPlaceImageEntity> images = new ArrayList<>();
        for (int index = 0; index < sanitizedImageUrls.size(); index++) {
            images.add(TravelPlaceImageEntity.builder()
                    .place(place)
                    .imageUrl(sanitizedImageUrls.get(index))
                    .main(index == 0)
                    .build());
        }
        this.travelPlaceImageJpaRepository.saveAll(images);
    }

    private List<String> sanitizeImageUrls(List<String> imageUrls) {
        if (imageUrls == null) {
            return List.of();
        }

        return new ArrayList<>(imageUrls.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll));
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private ProvinceResponse toProvinceResponse(ProvinceEntity province) {
        return new ProvinceResponse(province.getId(), province.getName(), province.getImage());
    }

    private TravelPlaceListItemResponse toListItemResponse(TravelPlaceEntity place,
            List<TravelPlaceImageResponse> images,
            TravelPlaceReviewSummaryResponse summary) {
        String mainImage = images.stream()
                .filter(TravelPlaceImageResponse::main)
                .map(TravelPlaceImageResponse::imageUrl)
                .findFirst()
                .orElseGet(() -> images.stream()
                        .map(TravelPlaceImageResponse::imageUrl)
                        .findFirst()
                        .orElse(null));
        return new TravelPlaceListItemResponse(
                place.getId(),
                place.getName(),
                place.getDescription(),
                toProvinceResponse(place.getProvince()),
                mainImage,
                images,
                place.getViews(),
                place.getOpeningTime(),
                summary.averageRating(),
                summary.reviewCount());
    }

    private TravelPlaceReviewSummaryResponse getReviewSummary(Long placeId) {
        try {
            TravelPlaceReviewStatsProjection stats = this.travelPlaceReviewJpaRepository.getStatsByPlaceId(placeId);
            double averageRating = stats == null || stats.getAverageRating() == null ? 0.0 : stats.getAverageRating();
            long reviewCount = stats == null || stats.getReviewCount() == null ? 0L : stats.getReviewCount();
            return new TravelPlaceReviewSummaryResponse(averageRating, reviewCount);
        } catch (Exception exception) {
            log.warn("Unable to load review summary for placeId={}", placeId, exception);
            return new TravelPlaceReviewSummaryResponse(0.0, 0L);
        }
    }

    private Map<Long, TravelPlaceReviewSummaryResponse> resolveReviewSummaryByPlaceId(Collection<Long> placeIds) {
        if (placeIds == null || placeIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, TravelPlaceReviewSummaryResponse> summaries = new LinkedHashMap<>();
        for (TravelPlaceReviewStatsProjection stats : this.travelPlaceReviewJpaRepository
                .getStatsByPlaceIds(placeIds)) {
            Long placeId = stats.getPlaceId();
            if (placeId == null) {
                continue;
            }
            summaries.put(placeId, new TravelPlaceReviewSummaryResponse(
                    stats.getAverageRating() == null ? 0.0 : stats.getAverageRating(),
                    stats.getReviewCount() == null ? 0L : stats.getReviewCount()));
        }
        return summaries;
    }

    private TravelPlaceReviewSummaryResponse emptyReviewSummary() {
        return new TravelPlaceReviewSummaryResponse(0.0, 0L);
    }

    private TravelPlaceReviewResponse toReviewResponse(TravelPlaceReviewEntity review) {
        TravelPlaceReviewAuthorResponse author = resolveReviewAuthor(review);

        return new TravelPlaceReviewResponse(
                review.getId(),
                author,
                review.getRating() == null ? 0 : review.getRating(),
                review.getContent() == null ? "" : review.getContent(),
                review.getCreatedAt(),
                review.getUpdatedAt());
    }

    private TravelPlaceReviewAuthorResponse resolveReviewAuthor(TravelPlaceReviewEntity review) {
        try {
            UserEntity user = review.getUser();
            if (user == null) {
                return UNKNOWN_REVIEW_AUTHOR;
            }

            String username = user.getUsername() == null || user.getUsername().isBlank()
                    ? "travelhub_user"
                    : user.getUsername();
            String displayName = user.getName() == null || user.getName().isBlank() ? username : user.getName();

            return new TravelPlaceReviewAuthorResponse(user.getId(), displayName, username, user.getAvatarUrl());
        } catch (EntityNotFoundException | JpaObjectRetrievalFailureException exception) {
            log.warn("Unable to resolve review author for reviewId={}", review.getId(), exception);
            return UNKNOWN_REVIEW_AUTHOR;
        }
    }

    private TravelPlaceReviewResponse resolveMyReview(Long placeId, Optional<Long> currentUserId) {
        if (currentUserId.isEmpty()) {
            return null;
        }

        try {
            return this.travelPlaceReviewJpaRepository.findByPlaceIdAndUserId(placeId, currentUserId.get())
                    .map(this::toReviewResponse)
                    .orElse(null);
        } catch (Exception exception) {
            log.warn("Unable to resolve current user's review for placeId={} userId={}",
                    placeId, currentUserId.get(), exception);
            return null;
        }
    }

    private boolean incrementViewsSafely(Long placeId) {
        try {
            this.sideEffectTransactionTemplate.executeWithoutResult(
                    status -> this.travelPlaceJpaRepository.incrementViews(placeId));
            return true;
        } catch (RuntimeException exception) {
            log.warn("Unable to increment travel place views for placeId={}", placeId, exception);
            return false;
        }
    }

    private void recordViewHistorySafely(Long placeId, Long userId) {
        try {
            this.sideEffectTransactionTemplate.executeWithoutResult(status -> {
                Optional<TravelPlaceViewHistoryEntity> latestHistory = this.travelPlaceViewHistoryJpaRepository
                        .findFirstByUserIdOrderByViewedAtDescIdDesc(userId);
                if (latestHistory.isPresent()
                        && Objects.equals(latestHistory.get().getPlace().getId(), placeId)) {
                    TravelPlaceViewHistoryEntity latest = latestHistory.get();
                    latest.setViewedAt(Instant.now());
                    this.travelPlaceViewHistoryJpaRepository.save(latest);
                    return;
                }
                TravelPlaceEntity placeReference = this.travelPlaceJpaRepository.getReferenceById(placeId);
                UserEntity userReference = this.userJpaRepository.getReferenceById(userId);
                this.travelPlaceViewHistoryJpaRepository.save(TravelPlaceViewHistoryEntity.builder()
                        .place(placeReference)
                        .user(userReference)
                        .build());
            });
        } catch (RuntimeException exception) {
            log.warn("Unable to record travel place view history for placeId={} userId={}",
                    placeId, userId, exception);
        }
    }

    private List<TravelPlaceViewHistoryEntity> collapseConsecutiveViewHistory(
            List<TravelPlaceViewHistoryEntity> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }

        List<TravelPlaceViewHistoryEntity> collapsed = new ArrayList<>();
        Long previousPlaceId = null;
        for (TravelPlaceViewHistoryEntity item : history) {
            Long placeId = item.getPlace().getId();
            if (Objects.equals(previousPlaceId, placeId)) {
                continue;
            }
            collapsed.add(item);
            previousPlaceId = placeId;
        }
        return collapsed;
    }

    private Map<Long, List<TravelPlaceImageResponse>> resolveImagesByPlaceId(Collection<Long> placeIds) {
        if (placeIds == null || placeIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<TravelPlaceImageResponse>> imagesByPlaceId = new LinkedHashMap<>();
        for (TravelPlaceImageEntity image : this.travelPlaceImageJpaRepository
                .findByPlaceIdInOrderByPlaceIdAscMainDescIdAsc(placeIds)) {
            Long placeId = image.getPlace().getId();
            imagesByPlaceId.computeIfAbsent(placeId, ignored -> new ArrayList<>())
                    .add(new TravelPlaceImageResponse(image.getId(), image.getImageUrl(), image.isMain()));
        }
        return imagesByPlaceId;
    }

    private TravelPlaceEntity findPlaceById(Long placeId) {
        return this.travelPlaceJpaRepository.findById(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Travel place not found"));
    }

    private void ensurePlaceExists(Long placeId) {
        if (!this.travelPlaceJpaRepository.existsById(placeId)) {
            throw new ResourceNotFoundException("Travel place not found");
        }
    }
}
