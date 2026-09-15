package ink.lucasnsnt.supernovaprojeto.dtos.student;

import jakarta.validation.constraints.AssertTrue;

import java.time.LocalTime;

public record ScheduleUpdateRequest(LocalTime outboundTime, LocalTime returnTime) {

    @AssertTrue(message = "informe ao menos um horário e mantenha a volta posterior à ida")
    public boolean isValid() {
        return (outboundTime != null || returnTime != null)
                && (outboundTime == null || returnTime == null || returnTime.isAfter(outboundTime));
    }
}
