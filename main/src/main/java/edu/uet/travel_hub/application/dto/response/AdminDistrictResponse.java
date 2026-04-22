package edu.uet.travel_hub.application.dto.response;

public record AdminDistrictResponse(
        Long id,
        Long provinceId,
        String name,
        String codename,
        String divisionType) {
}
