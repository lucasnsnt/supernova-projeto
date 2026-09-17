package ink.lucasnsnt.supernovaprojeto.dtos.route;

import ink.lucasnsnt.supernovaprojeto.models.enums.RouteEnrollmentStatus;
import jakarta.validation.constraints.NotNull;

public record RouteEnrollmentReviewRequest(@NotNull RouteEnrollmentStatus status) { }
