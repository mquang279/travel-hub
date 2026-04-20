package edu.uet.travel_hub.application.dto.response;

import java.util.List;

import lombok.Builder;

@Builder
public record PostResponse(Long id, String description, List<String> imageUrls, UserResponse owner) {

}
