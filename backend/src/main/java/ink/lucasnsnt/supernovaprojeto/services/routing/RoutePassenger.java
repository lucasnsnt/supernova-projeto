package ink.lucasnsnt.supernovaprojeto.services.routing;

import java.time.LocalDateTime;

public record RoutePassenger(
        Long confirmationId,
        Long studentId,
        String studentName,
        RoutePoint pickup,
        RoutePoint dropoff,
        LocalDateTime earliestPickupAt,
        LocalDateTime latestPickupAt,
        LocalDateTime latestDropoffAt,
        LocalDateTime preferredDropoffAt) {
}
