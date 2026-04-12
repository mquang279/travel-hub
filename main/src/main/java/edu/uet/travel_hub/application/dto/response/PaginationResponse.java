package edu.uet.travel_hub.application.dto.response;

import java.util.List;

public record PaginationResponse<T>(int pageNumber, int pageSize, int totalPages, Long totalElements, List<T> data) {

}
