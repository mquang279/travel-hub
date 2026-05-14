package edu.uet.travel_hub.application.port.in;

import edu.uet.travel_hub.application.dto.response.PaginationResponse;
import edu.uet.travel_hub.domain.model.NotificationModel;

public interface GetNotificationsUseCase {
    PaginationResponse<NotificationModel> get(int page, int pageSize);

    PaginationResponse<NotificationModel> getUnread(int page, int pageSize);
}
