package edu.uet.travel_hub.application.port.out;

import com.fasterxml.jackson.annotation.JsonProperty;

import edu.uet.travel_hub.application.dto.response.ItineraryAiProposalResponse;
import edu.uet.travel_hub.application.dto.response.ItineraryResponse;

public interface ItineraryAiGateway {
    ItineraryAiProposalResponse createProposal(ItineraryAiGatewayRequest request);

    record ItineraryAiGatewayRequest(
            String task,
            String prompt,
            @JsonProperty("input_type")
            String inputType,
            @JsonProperty("selected_day_id")
            Long selectedDayId,
            @JsonProperty("selected_day_index")
            Integer selectedDayIndex,
            @JsonProperty("desired_days")
            Integer desiredDays,
            String destination,
            ItineraryResponse itinerary) {
    }
}
