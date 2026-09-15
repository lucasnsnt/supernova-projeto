package ink.lucasnsnt.supernovaprojeto.dtos.notification;

import ink.lucasnsnt.supernovaprojeto.models.InAppNotification;
import ink.lucasnsnt.supernovaprojeto.models.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        Long tripId,
        Long confirmationId,
        LocalDateTime createdAt,
        LocalDateTime readAt) {

    public static NotificationResponse from(InAppNotification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getTrip() == null ? null : notification.getTrip().getId(),
                notification.getConfirmation() == null ? null : notification.getConfirmation().getId(),
                notification.getCreatedAt(),
                notification.getReadAt());
    }
}
