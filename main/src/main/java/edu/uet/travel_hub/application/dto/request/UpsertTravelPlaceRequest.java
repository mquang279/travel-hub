package edu.uet.travel_hub.application.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpsertTravelPlaceRequest(
        @NotNull Long provinceId,
        @NotBlank String name,
        @Size(max = 5000) String description,
        Double lat,
        Double lon,
        @Size(max = 255) String openingTime,
        List<@NotBlank String> imageUrls) {
}
