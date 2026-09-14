package ink.lucasnsnt.supernovaprojeto.dtos.student;

import ink.lucasnsnt.supernovaprojeto.models.enums.Direction;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ScheduleResponse(Long id, DayOfWeek dayOfWeek, LocalTime time, Direction direction) {
}
