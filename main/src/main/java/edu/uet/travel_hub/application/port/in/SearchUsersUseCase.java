package edu.uet.travel_hub.application.port.in;

import edu.uet.travel_hub.application.dto.response.PaginationResponse;
import edu.uet.travel_hub.domain.dto.response.UserProfileResponse;

public interface SearchUsersUseCase {
    PaginationResponse<UserProfileResponse> searchByUsername(String username, int pageNumber, int pageSize);
}
