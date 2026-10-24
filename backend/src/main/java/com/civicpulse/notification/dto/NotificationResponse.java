package com.civicpulse.notification.dto;

import com.civicpulse.notification.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private UUID id;
    private UUID userId;
    private UUID complaintId;
    private String title;
    private String message;
    private NotificationType type;
    private boolean isRead;
    private Instant createdAt;
}
