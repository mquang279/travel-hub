package edu.uet.travel_hub.application.dto.request;

import edu.uet.travel_hub.domain.enums.TripMemberRole;
import jakarta.validation.constraints.NotNull;

public record UpdateTripMemberRoleRequest(
        @NotNull TripMemberRole role) {
}
