package ink.lucasnsnt.supernovaprojeto.dtos.trip;

import ink.lucasnsnt.supernovaprojeto.models.TripParticipant;

import java.time.LocalDateTime;

public record TripParticipantResponse(
        Long studentId,
        String studentName,
        Long institutionId,
        String institutionName,
        int pickupOrder,
        int dropoffOrder,
        LocalDateTime estimatedPickupAt,
        LocalDateTime estimatedDropoffAt) {

    public static TripParticipantResponse from(TripParticipant participant) {
        var institution = participant.getStudent().getInstitution();
        return new TripParticipantResponse(
                participant.getStudent().getId(),
                participant.getStudent().getUser().getName(),
                institution == null ? null : institution.getId(),
                institution == null ? null : institution.getName(),
                participant.getPickupOrder(),
                participant.getDropoffOrder(),
                participant.getEstimatedPickupAt(),
                participant.getEstimatedDropoffAt());
    }
}
