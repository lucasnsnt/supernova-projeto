package ink.lucasnsnt.supernovaprojeto.dtos.route;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record RouteEnrollmentRequest(@NotNull Long routeId, boolean outboundEnabled, boolean returnEnabled) {
    @AssertTrue(message = "Selecione ida, volta ou ambas")
    public boolean hasDirection() { return outboundEnabled || returnEnabled; }
}
