package ink.lucasnsnt.supernovaprojeto.services;

import ink.lucasnsnt.supernovaprojeto.config.DailyTransportProperties;
import ink.lucasnsnt.supernovaprojeto.dtos.trip.TripResponse;
import ink.lucasnsnt.supernovaprojeto.exceptions.BusinessRuleException;
import ink.lucasnsnt.supernovaprojeto.exceptions.ResourceNotFoundException;
import ink.lucasnsnt.supernovaprojeto.models.Trip;
import ink.lucasnsnt.supernovaprojeto.models.Vehicle;
import ink.lucasnsnt.supernovaprojeto.models.enums.NotificationType;
import ink.lucasnsnt.supernovaprojeto.models.enums.TripStatus;
import ink.lucasnsnt.supernovaprojeto.repositories.TripRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.VehicleRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverService driverService;
    private final InAppNotificationService notificationService;
    private final DailyTransportProperties properties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<TripResponse> findByDriver(@NotNull Long driverId, @NotNull LocalDate date) {
        driverService.requireOperationalView(driverId);
        return tripRepository.findAllByDriverIdAndServiceDateOrderByPlannedDepartureAt(driverId, date)
                .stream().map(TripResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TripResponse> findByStudent(@NotNull Long studentId, @NotNull LocalDate date) {
        return tripRepository.findDistinctByParticipantsStudentIdAndServiceDateOrderByPlannedDepartureAt(
                        studentId, date)
                .stream().map(trip -> TripResponse.fromForStudent(trip, studentId)).toList();
    }

    @Transactional
    public TripResponse updateDeparture(
            @NotNull Long driverId,
            @NotNull Long tripId,
            @NotNull LocalDateTime departureAt,
            String reason) {
        Trip trip = findOwnedTrip(driverId, tripId);
        requireBeforeStart(trip);
        if (trip.getPlannedDepartureAt() == null) {
            throw new BusinessRuleException("A viagem ainda não possui um horário planejado");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (departureAt.isBefore(now)) {
            throw new BusinessRuleException("O horário de saída não pode estar no passado");
        }
        LocalDateTime lockAt = trip.getPlannedDepartureAt().minus(properties.getDepartureChangeLock());
        LocalDateTime currentDeparture = trip.getDriverDepartureAt() == null
                ? trip.getPlannedDepartureAt() : trip.getDriverDepartureAt();
        if (now.isBefore(lockAt)) {
            Duration adjustment = Duration.between(trip.getPlannedDepartureAt(), departureAt).abs();
            if (adjustment.compareTo(properties.getMaximumDepartureAdjustment()) > 0) {
                throw new BusinessRuleException("O ajuste de horário excede o limite permitido");
            }
        } else {
            if (!departureAt.isAfter(currentDeparture)) {
                throw new BusinessRuleException("Após o limite, somente atrasos podem ser informados");
            }
            if (reason == null || reason.isBlank()) {
                throw new BusinessRuleException("Informe o motivo do atraso");
            }
        }
        trip.setDriverDepartureAt(departureAt);
        trip.setDepartureConfirmedAt(now);
        notifyParticipants(trip, NotificationType.DEPARTURE_TIME_CHANGED,
                "Horário da viagem atualizado",
                "A saída da viagem foi atualizada para " + departureAt.toLocalTime());
        return TripResponse.from(trip);
    }

    @Transactional
    public TripResponse changeVehicle(
            @NotNull Long driverId, @NotNull Long tripId, @NotNull Long vehicleId) {
        Trip trip = findOwnedTrip(driverId, tripId);
        requireBeforeStart(trip);
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .filter(found -> found.getDriver().getId().equals(driverId))
                .orElseThrow(() -> new ResourceNotFoundException("Veículo", vehicleId));
        if (vehicle.getPassengerCapacity() < trip.getParticipants().size()) {
            throw new BusinessRuleException("O veículo não possui capacidade para os alunos confirmados");
        }
        trip.setVehicle(vehicle);
        notifyParticipants(trip, NotificationType.VEHICLE_CHANGED,
                "Veículo da viagem alterado",
                "O veículo será " + vehicle.getModel() + ", placa " + vehicle.getLicensePlate());
        return TripResponse.from(trip);
    }

    @Transactional
    public TripResponse start(@NotNull Long driverId, @NotNull Long tripId) {
        Trip trip = findOwnedTrip(driverId, tripId);
        if (trip.getStatus() != TripStatus.PLANNED) {
            throw new BusinessRuleException("Somente uma viagem planejada pode ser iniciada");
        }
        trip.setStatus(TripStatus.IN_PROGRESS);
        trip.setStartedAt(LocalDateTime.now(clock));
        return TripResponse.from(trip);
    }

    @Transactional
    public TripResponse complete(@NotNull Long driverId, @NotNull Long tripId) {
        Trip trip = findOwnedTrip(driverId, tripId);
        if (trip.getStatus() != TripStatus.IN_PROGRESS) {
            throw new BusinessRuleException("Somente uma viagem em andamento pode ser concluída");
        }
        trip.setStatus(TripStatus.COMPLETED);
        trip.setCompletedAt(LocalDateTime.now(clock));
        return TripResponse.from(trip);
    }

    @Transactional
    public TripResponse cancel(
            @NotNull Long driverId, @NotNull Long tripId, @NotBlank String reason) {
        Trip trip = findOwnedTrip(driverId, tripId);
        requireBeforeStart(trip);
        trip.setStatus(TripStatus.CANCELLED);
        trip.setCancellationReason(reason.trim());
        trip.setCancelledAt(LocalDateTime.now(clock));
        notifyParticipants(trip, NotificationType.TRIP_CANCELLED,
                "Viagem cancelada", "A viagem foi cancelada: " + reason.trim());
        return TripResponse.from(trip);
    }

    private Trip findOwnedTrip(Long driverId, Long tripId) {
        driverService.requireApproved(driverId);
        return tripRepository.findByIdAndDriverId(tripId, driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Viagem", tripId));
    }

    private void requireBeforeStart(Trip trip) {
        if (trip.getStatus() != TripStatus.PLANNED
                && trip.getStatus() != TripStatus.NEEDS_ATTENTION) {
            throw new BusinessRuleException("Esta operação só pode ser realizada antes do início da viagem");
        }
    }

    private void notifyParticipants(
            Trip trip, NotificationType type, String title, String message) {
        trip.getParticipants().forEach(participant -> notificationService.create(
                participant.getStudent().getId(), type, title, message, trip, participant.getConfirmation()));
    }
}
