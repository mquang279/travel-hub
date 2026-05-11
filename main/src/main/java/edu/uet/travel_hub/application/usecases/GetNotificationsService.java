package edu.uet.travel_hub.application.usecases;

import java.util.List;

import org.springframework.stereotype.Service;

import edu.uet.travel_hub.application.dto.response.PaginationResponse;
import edu.uet.travel_hub.application.port.in.GetNotificationsUseCase;
import edu.uet.travel_hub.application.port.out.CurrentUserProvider;
import edu.uet.travel_hub.application.port.out.NotificationRepository;
import edu.uet.travel_hub.domain.model.NotificationModel;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class GetNotificationsService implements GetNotificationsUseCase {
    private final NotificationRepository notificationRepository;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public PaginationResponse<NotificationModel> get(int page, int pageSize) {
        Long userId = this.currentUserProvider.getCurrentUserId();
        return this.notificationRepository.get(userId, page, pageSize);
    }

}
