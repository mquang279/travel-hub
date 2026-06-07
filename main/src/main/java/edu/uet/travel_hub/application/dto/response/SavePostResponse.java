package edu.uet.travel_hub.application.dto.response;

public record SavePostResponse(Long postId, Boolean saved, int saveCount) {
}
