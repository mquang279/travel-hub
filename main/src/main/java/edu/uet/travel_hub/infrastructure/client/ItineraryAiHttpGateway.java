package edu.uet.travel_hub.infrastructure.client;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import edu.uet.travel_hub.application.dto.response.ItineraryAiProposalResponse;
import edu.uet.travel_hub.application.port.out.ItineraryAiGateway;

@Component
public class ItineraryAiHttpGateway implements ItineraryAiGateway {
    private final WebClient aiWebClient;

    public ItineraryAiHttpGateway(WebClient aiWebClient) {
        this.aiWebClient = aiWebClient;
    }

    @Override
    public ItineraryAiProposalResponse createProposal(ItineraryAiGatewayRequest request) {
        return this.aiWebClient.post()
                .uri("/api/itineraries/proposals")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ItineraryAiProposalResponse.class)
                .block();
    }
}
