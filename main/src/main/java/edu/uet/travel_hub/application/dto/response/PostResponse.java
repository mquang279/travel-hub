package edu.uet.travel_hub.application.dto.response;

import lombok.Builder;

@Builder
public record PostResponse(Long id, String description, String imageUrl, UserResponse owner) {

}
