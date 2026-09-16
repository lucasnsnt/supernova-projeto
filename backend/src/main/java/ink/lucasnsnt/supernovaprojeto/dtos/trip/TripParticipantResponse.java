package ink.lucasnsnt.supernovaprojeto.dtos.trip;

import ink.lucasnsnt.supernovaprojeto.models.TripParticipant;
import ink.lucasnsnt.supernovaprojeto.dtos.common.AddressResponse;
import ink.lucasnsnt.supernovaprojeto.models.enums.Direction;

import java.time.LocalDateTime;

public record TripParticipantResponse(
        Long studentId,
        String studentName,
        Long institutionId,
        String institutionName,
        int pickupOrder,
        int dropoffOrder,
        LocalDateTime estimatedPickupAt,
        LocalDateTime estimatedDropoffAt,
        AddressResponse pickupAddress,
        AddressResponse dropoffAddress) {

    public static TripParticipantResponse from(TripParticipant participant, Direction direction) {
        var institution = participant.getStudent().getInstitution();
        var home = participant.getStudent().getUser().getAddress();
        var destination = institution == null ? null : institution.getAddress();
        return new TripParticipantResponse(
                participant.getStudent().getId(),
                participant.getStudent().getUser().getName(),
                institution == null ? null : institution.getId(),
                institution == null ? null : institution.getName(),
                participant.getPickupOrder(),
                participant.getDropoffOrder(),
                participant.getEstimatedPickupAt(),
                participant.getEstimatedDropoffAt(),
                AddressResponse.from(direction == Direction.IDA ? home : destination),
                AddressResponse.from(direction == Direction.IDA ? destination : home));
    }
}
