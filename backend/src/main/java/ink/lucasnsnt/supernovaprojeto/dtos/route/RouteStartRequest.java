package ink.lucasnsnt.supernovaprojeto.dtos.route;
import ink.lucasnsnt.supernovaprojeto.models.enums.Direction;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
public record RouteStartRequest(@NotNull LocalDate serviceDate, @NotNull Direction direction, boolean acknowledgeOutsideWindow) {}
