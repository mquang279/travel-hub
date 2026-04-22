package edu.uet.travel_hub.application.dto.response;

public record AdminProvinceResponse(
        Long id,
        String name,
        String codename,
        String divisionType,
        Integer phoneCode,
        String image) {
}
