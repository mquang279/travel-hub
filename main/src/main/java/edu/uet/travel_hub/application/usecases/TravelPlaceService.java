package edu.uet.travel_hub.application.usecases;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ProvinceJpaRepository provinceJpaRepository;
    private final TravelPlaceJpaRepository travelPlaceJpaRepository;
    private final TravelPlaceImageJpaRepository travelPlaceImageJpaRepository;
    private final TravelPlaceReviewJpaRepository travelPlaceReviewJpaRepository;
    private final TravelPlaceViewHistoryJpaRepository travelPlaceViewHistoryJpaRepository;
    private final UserJpaRepository userJpaRepository;

    public TravelPlaceService(
            ProvinceJpaRepository provinceJpaRepository,
            TravelPlaceJpaRepository travelPlaceJpaRepository,
            TravelPlaceImageJpaRepository travelPlaceImageJpaRepository,
            TravelPlaceReviewJpaRepository travelPlaceReviewJpaRepository,
            TravelPlaceViewHistoryJpaRepository travelPlaceViewHistoryJpaRepository,
            UserJpaRepository userJpaRepository) {
        this.provinceJpaRepository = provinceJpaRepository;
        this.travelPlaceJpaRepository = travelPlaceJpaRepository;
        this.travelPlaceImageJpaRepository = travelPlaceImageJpaRepository;
        this.travelPlaceReviewJpaRepository = travelPlaceReviewJpaRepository;
        this.travelPlaceViewHistoryJpaRepository = travelPlaceViewHistoryJpaRepository;
        this.userJpaRepository = userJpaRepository;
    }

    @Transactional(readOnly = true)
    public PaginationResponse<TravelPlaceListItemResponse> getPlaces(int page, int pageSize, Long provinceId, String keyword) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<TravelPlaceEntity> places = this.travelPlaceJpaRepository.search(provinceId, normalizeKeyword(keyword), pageable);

        Map<Long, String> mainImages = resolveMainImageByPlaceId(places.stream().map(TravelPlaceEntity::getId).toList());
        List<TravelPlaceListItemResponse> data = places.getContent().stream()
                .map(place -> toListItemResponse(place, mainImages.get(place.getId()), getReviewSummary(place.getId())))
                .toList();

        return new PaginationResponse<>(places.getNumber(), places.getSize(), places.getTotalPages(),
                places.getTotalElements(), data);
    }

    @Transactional
    public TravelPlaceDetailResponse getPlaceDetail(Long placeId, Optional<Long> currentUserId) {
        TravelPlaceEntity place = this.travelPlaceJpaRepository.findById(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Travel place not found"));

        this.travelPlaceJpaRepository.incrementViews(placeId);
        place.setViews((place.getViews() == null ? 0 : place.getViews()) + 1);

        if (currentUserId.isPresent()) {
            UserEntity user = this.userJpaRepository.getReferenceById(currentUserId.get());
            this.travelPlaceViewHistoryJpaRepository.save(TravelPlaceViewHistoryEntity.builder()
                    .place(place)
                    .user(user)
                    .build());
        }

        return buildPlaceDetailResponse(place, currentUserId);
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
    public TravelPlaceReviewResponse upsertReview(Long placeId, Long currentUserId, UpsertTravelPlaceReviewRequest request) {
        TravelPlaceEntity place = this.travelPlaceJpaRepository.findById(placeId)
                .orElseThrow(() -> new ResourceNotFoundException("Travel place not found"));
        UserEntity user = this.userJpaRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TravelPlaceReviewEntity review = this.travelPlaceReviewJpaRepository.findByPlaceIdAndUserId(placeId, currentUserId)
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
    public PaginationResponse<TravelPlaceViewHistoryResponse> getViewHistory(Long currentUserId, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "viewedAt", "id"));
        Page<TravelPlaceViewHistoryEntity> history = this.travelPlaceViewHistoryJpaRepository
                .findByUserIdOrderByViewedAtDescIdDesc(currentUserId, pageable);

        Map<Long, String> mainImages = resolveMainImageByPlaceId(
                history.getContent().stream().map(item -> item.getPlace().getId()).toList());

        List<TravelPlaceViewHistoryResponse> data = history.getContent().stream()
                .map(item -> new TravelPlaceViewHistoryResponse(
                        item.getPlace().getId(),
                        item.getPlace().getName(),
                        mainImages.get(item.getPlace().getId()),
                        item.getPlace().getProvince().getName(),
                        item.getViewedAt()))
                .toList();

        return new PaginationResponse<>(history.getNumber(), history.getSize(), history.getTotalPages(),
                history.getTotalElements(), data);
    }

    private TravelPlaceDetailResponse buildPlaceDetailResponse(TravelPlaceEntity place, Optional<Long> currentUserId) {
        List<TravelPlaceImageResponse> images = this.travelPlaceImageJpaRepository.findByPlaceIdOrderByMainDescIdAsc(place.getId())
                .stream()
                .map(image -> new TravelPlaceImageResponse(image.getId(), image.getImageUrl(), image.isMain()))
                .toList();

        TravelPlaceReviewSummaryResponse reviewSummary = getReviewSummary(place.getId());
        TravelPlaceReviewResponse myReview = currentUserId
                .flatMap(userId -> this.travelPlaceReviewJpaRepository.findByPlaceIdAndUserId(place.getId(), userId))
                .map(this::toReviewResponse)
                .orElse(null);

        return new TravelPlaceDetailResponse(
                place.getId(),
                place.getName(),
                place.getDescription(),
                place.getLat(),
                place.getLon(),
                place.getViews(),
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

    private TravelPlaceListItemResponse toListItemResponse(TravelPlaceEntity place, String mainImage,
            TravelPlaceReviewSummaryResponse summary) {
        return new TravelPlaceListItemResponse(
                place.getId(),
                place.getName(),
                place.getDescription(),
                toProvinceResponse(place.getProvince()),
                mainImage,
                place.getViews(),
                place.getOpeningTime(),
                summary.averageRating(),
                summary.reviewCount());
    }

    private TravelPlaceReviewSummaryResponse getReviewSummary(Long placeId) {
        TravelPlaceReviewStatsProjection stats = this.travelPlaceReviewJpaRepository.getStatsByPlaceId(placeId);
        double averageRating = stats == null || stats.getAverageRating() == null ? 0.0 : stats.getAverageRating();
        long reviewCount = stats == null || stats.getReviewCount() == null ? 0L : stats.getReviewCount();
        return new TravelPlaceReviewSummaryResponse(averageRating, reviewCount);
    }

    private TravelPlaceReviewResponse toReviewResponse(TravelPlaceReviewEntity review) {
        UserEntity user = review.getUser();
        String displayName = user.getName() == null || user.getName().isBlank() ? user.getUsername() : user.getName();

        return new TravelPlaceReviewResponse(
                review.getId(),
                new TravelPlaceReviewAuthorResponse(user.getId(), displayName, user.getUsername(), user.getAvatarUrl()),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt(),
                review.getUpdatedAt());
    }

    private Map<Long, String> resolveMainImageByPlaceId(Collection<Long> placeIds) {
        if (placeIds == null || placeIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> imageByPlaceId = new LinkedHashMap<>();
        for (TravelPlaceImageEntity image : this.travelPlaceImageJpaRepository.findByPlaceIdInOrderByPlaceIdAscMainDescIdAsc(placeIds)) {
            imageByPlaceId.putIfAbsent(image.getPlace().getId(), image.getImageUrl());
        }
        return imageByPlaceId;
    }

    private void ensurePlaceExists(Long placeId) {
        if (!this.travelPlaceJpaRepository.existsById(placeId)) {
            throw new ResourceNotFoundException("Travel place not found");
        }
    }
}
