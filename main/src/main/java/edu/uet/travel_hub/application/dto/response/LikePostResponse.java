package edu.uet.travel_hub.application.dto.response;

public record LikePostResponse(Long postId, Boolean liked, int likeCount) {

}
