package ink.lucasnsnt.supernovaprojeto.services;

import ink.lucasnsnt.supernovaprojeto.dtos.trip.*;
import ink.lucasnsnt.supernovaprojeto.exceptions.BusinessRuleException;
import ink.lucasnsnt.supernovaprojeto.exceptions.ResourceNotFoundException;
import ink.lucasnsnt.supernovaprojeto.models.Trip;
import ink.lucasnsnt.supernovaprojeto.models.TripLocation;
import ink.lucasnsnt.supernovaprojeto.models.enums.TripStatus;
import ink.lucasnsnt.supernovaprojeto.repositories.TripLocationRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.TripRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@Validated
@RequiredArgsConstructor
public class TripTrackingService {
    private final TripRepository tripRepository;
    private final TripLocationRepository locationRepository;
    private final DriverService driverService;
    private final Clock clock;

    @Transactional
    public TripTrackingResponse updateLocation(@NotNull Long driverId, @NotNull Long tripId,
                                               @Valid TripLocationUpdateRequest request) {
        Trip trip = ownedTrip(driverId, tripId);
        requireInProgress(trip);
        LocalDateTime now = LocalDateTime.now(clock);
        TripLocation location = locationRepository.findById(tripId).orElseGet(() -> TripLocation.builder()
                .trip(trip).tripId(tripId).build());
        location.setLatitude(request.latitude());
        location.setLongitude(request.longitude());
        location.setAccuracy(request.accuracy());
        location.setHeading(request.heading());
        location.setRecordedAt(request.recordedAt() == null ? now : request.recordedAt());
        location.setUpdatedAt(now);
        locationRepository.save(location);
        return response(trip, location, null);
    }

    @Transactional(readOnly = true)
    public TripTrackingResponse findForDriver(@NotNull Long driverId, @NotNull Long tripId) {
        Trip trip = ownedTrip(driverId, tripId);
        return response(trip, activeLocation(trip), null);
    }

    @Transactional(readOnly = true)
    public TripTrackingResponse findForStudent(@NotNull Long studentId, @NotNull Long tripId) {
        Trip trip = tripRepository.findDistinctByIdAndParticipantsStudentId(tripId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Viagem", tripId));
        return response(trip, activeLocation(trip), studentId);
    }

    private Trip ownedTrip(Long driverId, Long tripId) {
        driverService.requireApproved(driverId);
        return tripRepository.findByIdAndDriverId(tripId, driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Viagem", tripId));
    }

    private TripLocation activeLocation(Trip trip) {
        if (trip.getStatus() != TripStatus.IN_PROGRESS) return null;
        return locationRepository.findById(trip.getId()).orElse(null);
    }

    private void requireInProgress(Trip trip) {
        if (trip.getStatus() != TripStatus.IN_PROGRESS) {
            throw new BusinessRuleException("A localização só pode ser compartilhada durante uma viagem em andamento");
        }
    }

    private TripTrackingResponse response(Trip trip, TripLocation location, Long studentId) {
        TripResponse tripResponse = studentId == null ? TripResponse.from(trip) : TripResponse.fromForStudent(trip, studentId);
        return new TripTrackingResponse(tripResponse, location == null ? null : TripLocationResponse.from(location));
    }
}
