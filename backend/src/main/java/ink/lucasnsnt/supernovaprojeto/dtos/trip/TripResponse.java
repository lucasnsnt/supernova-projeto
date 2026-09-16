package ink.lucasnsnt.supernovaprojeto.dtos.trip;

import ink.lucasnsnt.supernovaprojeto.dtos.vehicle.VehicleResponse;
import ink.lucasnsnt.supernovaprojeto.models.Trip;
import ink.lucasnsnt.supernovaprojeto.models.enums.Direction;
import ink.lucasnsnt.supernovaprojeto.models.enums.TripStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TripResponse(
        Long id,
        LocalDate serviceDate,
        Direction direction,
        TripStatus status,
        LocalDateTime plannedDepartureAt,
        LocalDateTime departureAt,
        LocalDateTime departureConfirmedAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt,
        String cancellationReason,
        String planningIssue,
        VehicleResponse vehicle,
        String encodedPolyline,
        List<TripParticipantResponse> participants) {

    public static TripResponse from(Trip trip) {
        return from(trip, null);
    }

    public static TripResponse fromForStudent(Trip trip, Long studentId) {
        return from(trip, studentId);
    }

    private static TripResponse from(Trip trip, Long visibleStudentId) {
        return new TripResponse(
                trip.getId(),
                trip.getServiceDate(),
                trip.getDirection(),
                trip.getStatus(),
                trip.getPlannedDepartureAt(),
                trip.getDriverDepartureAt() == null
                        ? trip.getPlannedDepartureAt() : trip.getDriverDepartureAt(),
                trip.getDepartureConfirmedAt(),
                trip.getStartedAt(),
                trip.getCompletedAt(),
                trip.getCancelledAt(),
                trip.getCancellationReason(),
                trip.getPlanningIssue(),
                trip.getVehicle() == null ? null : VehicleResponse.from(trip.getVehicle()),
                trip.getEncodedPolyline(),
                trip.getParticipants().stream()
                        .filter(participant -> visibleStudentId == null
                                || participant.getStudent().getId().equals(visibleStudentId))
                        .map(participant -> TripParticipantResponse.from(participant, trip.getDirection()))
                        .toList());
    }
}
