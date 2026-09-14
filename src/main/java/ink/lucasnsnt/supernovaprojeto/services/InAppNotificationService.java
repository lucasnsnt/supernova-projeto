package ink.lucasnsnt.supernovaprojeto.services;

import ink.lucasnsnt.supernovaprojeto.dtos.notification.NotificationResponse;
import ink.lucasnsnt.supernovaprojeto.events.InAppNotificationCreatedEvent;
import ink.lucasnsnt.supernovaprojeto.exceptions.ResourceNotFoundException;
import ink.lucasnsnt.supernovaprojeto.models.DailyConfirmation;
import ink.lucasnsnt.supernovaprojeto.models.InAppNotification;
import ink.lucasnsnt.supernovaprojeto.models.Trip;
import ink.lucasnsnt.supernovaprojeto.models.User;
import ink.lucasnsnt.supernovaprojeto.models.enums.NotificationType;
import ink.lucasnsnt.supernovaprojeto.repositories.InAppNotificationRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.UserRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
public class InAppNotificationService {

    private final InAppNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public NotificationResponse create(
            @NotNull Long recipientId,
            @NotNull NotificationType type,
            @NotBlank String title,
            @NotBlank String message,
            Trip trip,
            DailyConfirmation confirmation) {
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", recipientId));
        InAppNotification notification = notificationRepository.save(InAppNotification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .message(message)
                .trip(trip)
                .confirmation(confirmation)
                .createdAt(LocalDateTime.now(clock))
                .build());
        NotificationResponse response = NotificationResponse.from(notification);
        eventPublisher.publishEvent(new InAppNotificationCreatedEvent(recipientId, response));
        return response;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> findAll(@NotNull Long recipientId) {
        return notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(recipientId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countUnread(@NotNull Long recipientId) {
        return notificationRepository.countByRecipientIdAndReadAtIsNull(recipientId);
    }

    @Transactional
    public NotificationResponse markAsRead(@NotNull Long recipientId, @NotNull Long notificationId) {
        InAppNotification notification = notificationRepository.findByIdAndRecipientId(notificationId, recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação", notificationId));
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now(clock));
        }
        return NotificationResponse.from(notification);
    }

    @Transactional
    public void markAllAsRead(@NotNull Long recipientId) {
        LocalDateTime readAt = LocalDateTime.now(clock);
        notificationRepository.findAllByRecipientIdAndReadAtIsNull(recipientId)
                .forEach(notification -> notification.setReadAt(readAt));
    }
}
