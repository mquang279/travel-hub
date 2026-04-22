package edu.uet.travel_hub.application.dto.response;

public record AdminWardResponse(
        Long id,
        Long districtId,
        Long provinceId,
        String name,
        String codename,
        String divisionType) {
}
