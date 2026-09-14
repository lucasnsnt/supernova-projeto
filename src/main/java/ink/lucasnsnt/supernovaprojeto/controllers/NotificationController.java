package ink.lucasnsnt.supernovaprojeto.controllers;

import ink.lucasnsnt.supernovaprojeto.dtos.notification.NotificationResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.notification.UnreadNotificationCountResponse;
import ink.lucasnsnt.supernovaprojeto.services.InAppNotificationService;
import ink.lucasnsnt.supernovaprojeto.services.NotificationStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/me/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final InAppNotificationService notificationService;
    private final NotificationStreamService streamService;

    @GetMapping
    public List<NotificationResponse> findAll(@AuthenticationPrincipal Jwt jwt) {
        return notificationService.findAll(userId(jwt));
    }

    @GetMapping("/unread-count")
    public UnreadNotificationCountResponse countUnread(@AuthenticationPrincipal Jwt jwt) {
        return new UnreadNotificationCountResponse(notificationService.countUnread(userId(jwt)));
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal Jwt jwt) {
        return streamService.subscribe(userId(jwt));
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markAsRead(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long notificationId) {
        return notificationService.markAsRead(userId(jwt), notificationId);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        notificationService.markAllAsRead(userId(jwt));
        return ResponseEntity.noContent().build();
    }

    private Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
