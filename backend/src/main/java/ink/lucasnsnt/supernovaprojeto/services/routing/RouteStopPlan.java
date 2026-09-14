package ink.lucasnsnt.supernovaprojeto.services.routing;

import java.time.LocalDateTime;

public record RouteStopPlan(
        Long confirmationId,
        int pickupOrder,
        int dropoffOrder,
        LocalDateTime estimatedPickupAt,
        LocalDateTime estimatedDropoffAt) {
}
