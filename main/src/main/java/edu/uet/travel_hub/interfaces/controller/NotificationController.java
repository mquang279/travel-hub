package edu.uet.travel_hub.interfaces.controller;

import org.springframework.web.bind.annotation.RestController;

import edu.uet.travel_hub.application.dto.response.PaginationResponse;
import edu.uet.travel_hub.application.port.in.GetNotificationsUseCase;
import edu.uet.travel_hub.domain.model.NotificationModel;
import lombok.AllArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/notifications")
@AllArgsConstructor
public class NotificationController {
    private final GetNotificationsUseCase getNotificationsUseCase;

    @GetMapping("")
    public ResponseEntity<PaginationResponse<NotificationModel>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        PaginationResponse<NotificationModel> notifications = this.getNotificationsUseCase.get(page, pageSize);
        return ResponseEntity.ok().body(notifications);
    }
}
