package edu.uet.travel_hub.domain.model;

import java.time.Instant;

import lombok.Builder;

@Builder
public record NotificationModel(String title, String body, Boolean isRead, Instant createdAt) {

}
