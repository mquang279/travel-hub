package edu.uet.travel_hub.application.dto.request;

import java.util.List;

public record AddTripPhotosRequest(List<String> imageUrls) {
}
