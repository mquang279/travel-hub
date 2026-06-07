package edu.uet.travel_hub.interfaces.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.uet.travel_hub.application.dto.request.TravelAssistantChatRequest;
import edu.uet.travel_hub.application.dto.response.TravelAssistantChatResponse;
import edu.uet.travel_hub.infrastructure.client.AiTravelAssistantHttpGateway;
import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/ai/travel-assistant")
public class TravelAssistantController {
    private final AiTravelAssistantHttpGateway aiTravelAssistantHttpGateway;

    public TravelAssistantController(AiTravelAssistantHttpGateway aiTravelAssistantHttpGateway) {
        this.aiTravelAssistantHttpGateway = aiTravelAssistantHttpGateway;
    }

    @PostMapping("/chat")
    public ResponseEntity<TravelAssistantChatResponse> chat(
            @Valid @RequestBody TravelAssistantChatRequest request) {
        return ResponseEntity.ok(this.aiTravelAssistantHttpGateway.chat(request));
    }
}
