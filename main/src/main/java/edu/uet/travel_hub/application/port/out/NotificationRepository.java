package edu.uet.travel_hub.application.port.out;

import java.util.List;

import edu.uet.travel_hub.application.dto.response.PaginationResponse;
import edu.uet.travel_hub.domain.model.NotificationModel;

public interface NotificationRepository {
    void save(Long userId, String title, String body);

    PaginationResponse<NotificationModel> get(Long userId, int page, int pageSize);
}
