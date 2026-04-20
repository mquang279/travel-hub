package edu.uet.travel_hub.application.dto.request;

import java.util.List;

public record CreatePostRequest(String description, List<String> imageUrls, String location) {

}