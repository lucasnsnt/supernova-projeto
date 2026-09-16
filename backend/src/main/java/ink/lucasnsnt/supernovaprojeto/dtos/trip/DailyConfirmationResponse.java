package ink.lucasnsnt.supernovaprojeto.dtos.trip;

import ink.lucasnsnt.supernovaprojeto.models.DailyConfirmation;
import ink.lucasnsnt.supernovaprojeto.models.enums.DailyConfirmationStatus;
import ink.lucasnsnt.supernovaprojeto.models.enums.Direction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record DailyConfirmationResponse(
        Long id,
        Long driverId,
        String driverName,
        Long studentId,
        String studentName,
        String institutionName,
        LocalDate serviceDate,
        Direction direction,
        LocalTime scheduledTime,
        LocalDateTime preliminaryDepartureAt,
        LocalDateTime availableAt,
        LocalDateTime responseDeadline,
        DailyConfirmationStatus status,
        LocalDateTime respondedAt) {

    public static DailyConfirmationResponse from(DailyConfirmation confirmation) {
        return new DailyConfirmationResponse(
                confirmation.getId(),
                confirmation.getDriver().getId(),
                confirmation.getDriver().getUser().getName(),
                confirmation.getStudent().getId(),
                confirmation.getStudent().getUser().getName(),
                confirmation.getStudent().getInstitution() == null
                        ? null
                        : confirmation.getStudent().getInstitution().getName(),
                confirmation.getServiceDate(),
                confirmation.getDirection(),
                confirmation.getScheduledTime(),
                confirmation.getPreliminaryDepartureAt(),
                confirmation.getAvailableAt(),
                confirmation.getResponseDeadline(),
                confirmation.getStatus(),
                confirmation.getRespondedAt());
    }
}
