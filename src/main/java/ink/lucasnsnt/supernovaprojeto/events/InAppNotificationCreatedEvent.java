package ink.lucasnsnt.supernovaprojeto.events;

import ink.lucasnsnt.supernovaprojeto.dtos.notification.NotificationResponse;

public record InAppNotificationCreatedEvent(Long recipientId, NotificationResponse notification) {
}
