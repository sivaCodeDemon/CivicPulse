package com.civicpulse.notification;

import com.civicpulse.common.exception.ResourceNotFoundException;
import com.civicpulse.common.response.PagedResponse;
import com.civicpulse.complaint.Complaint;
import com.civicpulse.notification.dto.NotificationResponse;
import com.civicpulse.security.UserPrincipal;
import com.civicpulse.user.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationRepository notificationRepository;

    @Transactional
    public void sendNotification(User recipient, Complaint complaint, String title, String message, NotificationType type) {
        if (recipient == null) {
            return;
        }

        Notification notification = Notification.builder()
                .user(recipient)
                .complaint(complaint)
                .title(title)
                .message(message)
                .type(type)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
        log.info("Sent {} notification to user {}: '{}'", type, recipient.getEmail(), title);
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getUserNotifications(UserPrincipal currentUser, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByUserId(currentUser.getId(), pageable);
        List<NotificationResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PagedResponse.<NotificationResponse>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional
    public NotificationResponse markNotificationAsRead(UUID id, UserPrincipal currentUser) {
        Notification notification = notificationRepository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));

        notification.setRead(true);
        Notification updated = notificationRepository.save(notification);
        return mapToResponse(updated);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UserPrincipal currentUser) {
        return notificationRepository.countByUserIdAndIsReadFalse(currentUser.getId());
    }

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .userId(n.getUser().getId())
                .complaintId(n.getComplaint() != null ? n.getComplaint().getId() : null)
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
