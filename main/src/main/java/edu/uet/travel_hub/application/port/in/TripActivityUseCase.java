package edu.uet.travel_hub.application.port.in;

import java.util.List;

import edu.uet.travel_hub.application.dto.request.CreateTripActivityRequest;
import edu.uet.travel_hub.application.dto.request.UpdateTripActivityRequest;
import edu.uet.travel_hub.application.dto.response.TripActivityResponse;
import edu.uet.travel_hub.application.dto.response.TripDayResponse;

public interface TripActivityUseCase {
    List<TripDayResponse> listTripDays(Long tripId, Long currentUserId);

    TripActivityResponse createActivity(Long tripId, Long currentUserId, CreateTripActivityRequest request);

    TripActivityResponse updateActivity(
            Long tripId,
            Long activityId,
            Long currentUserId,
            UpdateTripActivityRequest request);

    void deleteActivity(Long tripId, Long activityId, Long currentUserId);
}
