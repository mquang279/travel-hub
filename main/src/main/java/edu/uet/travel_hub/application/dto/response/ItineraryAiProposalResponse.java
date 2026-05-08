package edu.uet.travel_hub.application.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ItineraryAiProposalResponse(
        @JsonProperty("proposal_id")
        String proposalId,
        @JsonProperty("base_version")
        int baseVersion,
        String summary,
        List<ItineraryAiChangeResponse> changes) {
}
