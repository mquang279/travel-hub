package edu.uet.travel_hub.application.port.in;

import java.util.List;

import edu.uet.travel_hub.application.dto.request.ApplyItineraryAiProposalRequest;
import edu.uet.travel_hub.application.dto.request.CreateItineraryAiProposalRequest;
import edu.uet.travel_hub.application.dto.request.CreateItineraryDayRequest;
import edu.uet.travel_hub.application.dto.request.CreateItineraryRequest;
import edu.uet.travel_hub.application.dto.request.CreateItineraryStopRequest;
import edu.uet.travel_hub.application.dto.request.UpdateItineraryDayRequest;
import edu.uet.travel_hub.application.dto.request.UpdateItineraryRequest;
import edu.uet.travel_hub.application.dto.request.UpdateItineraryStopRequest;
import edu.uet.travel_hub.application.dto.response.ItineraryAiProposalResponse;
import edu.uet.travel_hub.application.dto.response.ItineraryResponse;
import edu.uet.travel_hub.application.dto.response.ItinerarySummaryResponse;

public interface ItineraryUseCase {
    List<ItinerarySummaryResponse> getMyItineraries(Long currentUserId);

    ItineraryResponse getItinerary(Long itineraryId, Long currentUserId);

    ItineraryResponse getItineraryByGroupName(String groupName, Long currentUserId);

    ItineraryResponse createItinerary(Long currentUserId, CreateItineraryRequest request);

    ItineraryResponse updateItinerary(Long itineraryId, Long currentUserId, UpdateItineraryRequest request);

    void deleteItinerary(Long itineraryId, Long currentUserId);

    ItineraryResponse createDay(Long itineraryId, Long currentUserId, CreateItineraryDayRequest request);

    ItineraryResponse updateDay(
            Long itineraryId,
            Long dayId,
            Long currentUserId,
            UpdateItineraryDayRequest request);

    ItineraryResponse deleteDay(Long itineraryId, Long dayId, Long currentUserId);

    ItineraryResponse createStop(Long itineraryId, Long currentUserId, CreateItineraryStopRequest request);

    ItineraryResponse updateStop(
            Long itineraryId,
            Long stopId,
            Long currentUserId,
            UpdateItineraryStopRequest request);

    ItineraryResponse deleteStop(Long itineraryId, Long stopId, Long currentUserId);

    ItineraryAiProposalResponse createAiProposal(
            Long itineraryId,
            Long currentUserId,
            CreateItineraryAiProposalRequest request);

    ItineraryResponse applyAiProposal(
            Long itineraryId,
            String proposalId,
            Long currentUserId,
            ApplyItineraryAiProposalRequest request);
}
