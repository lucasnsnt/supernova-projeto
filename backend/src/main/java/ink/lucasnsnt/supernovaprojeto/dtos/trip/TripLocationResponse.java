package ink.lucasnsnt.supernovaprojeto.dtos.trip;

import ink.lucasnsnt.supernovaprojeto.models.TripLocation;

import java.time.LocalDateTime;

public record TripLocationResponse(
        double latitude, double longitude, Double accuracy, Double heading,
        LocalDateTime recordedAt, LocalDateTime updatedAt) {
    public static TripLocationResponse from(TripLocation location) {
        return new TripLocationResponse(location.getLatitude(), location.getLongitude(),
                location.getAccuracy(), location.getHeading(), location.getRecordedAt(), location.getUpdatedAt());
    }
}
