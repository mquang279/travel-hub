package edu.uet.travel_hub.application.port.out;

import java.util.List;
import java.util.Optional;

import edu.uet.travel_hub.domain.model.ItineraryDetailRowModel;
import edu.uet.travel_hub.domain.model.ItineraryModel;
import edu.uet.travel_hub.domain.model.ItinerarySummaryModel;

public interface ItineraryRepository {
    List<ItinerarySummaryModel> findSummariesByOwnerId(Long ownerId);

    Optional<ItineraryModel> findByIdAndOwnerId(Long id, Long ownerId);

    Optional<ItineraryModel> findByOwnerIdAndGroupNameIgnoreCase(Long ownerId, String groupName);

    boolean existsByOwnerIdAndGroupNameIgnoreCase(Long ownerId, String groupName);

    boolean existsByOwnerIdAndGroupNameIgnoreCaseAndIdNot(Long ownerId, String groupName, Long id);

    ItineraryModel saveAndFlush(ItineraryModel itinerary);

    void delete(ItineraryModel itinerary);

    List<ItineraryDetailRowModel> findDetailRowsByIdAndOwnerId(Long id, Long ownerId);
}
