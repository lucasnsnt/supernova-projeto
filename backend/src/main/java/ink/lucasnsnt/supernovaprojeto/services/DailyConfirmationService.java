package ink.lucasnsnt.supernovaprojeto.services;

import ink.lucasnsnt.supernovaprojeto.config.DailyTransportProperties;
import ink.lucasnsnt.supernovaprojeto.dtos.trip.DailyConfirmationResponse;
import ink.lucasnsnt.supernovaprojeto.exceptions.BusinessRuleException;
import ink.lucasnsnt.supernovaprojeto.exceptions.ResourceNotFoundException;
import ink.lucasnsnt.supernovaprojeto.models.DailyConfirmation;
import ink.lucasnsnt.supernovaprojeto.models.RecurringRouteEnrollment;
import ink.lucasnsnt.supernovaprojeto.models.RecurringRouteSchedule;
import ink.lucasnsnt.supernovaprojeto.models.enums.Direction;
import ink.lucasnsnt.supernovaprojeto.repositories.RecurringRouteEnrollmentRepository;
import ink.lucasnsnt.supernovaprojeto.models.enums.*;
import ink.lucasnsnt.supernovaprojeto.repositories.DailyConfirmationRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.DriverStudentLinkRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
public class DailyConfirmationService {

    private final DailyConfirmationRepository confirmationRepository;
    private final DriverStudentLinkRepository linkRepository;
    private final RecurringRouteEnrollmentRepository routeEnrollmentRepository;
    private final InAppNotificationService notificationService;
    private final DailyTransportProperties properties;
    private final Clock clock;
    private final ink.lucasnsnt.supernovaprojeto.repositories.DriverRepository driverRepository;
    private final ink.lucasnsnt.supernovaprojeto.repositories.TripRepository tripRepository;
    private final RouteEligibilityService eligibility;

    @Transactional
    public int releaseAvailableForDate(@NotNull LocalDate serviceDate) {
        LocalDateTime now = LocalDateTime.now(clock);
        int created = 0;
        for (RecurringRouteEnrollment enrollment : routeEnrollmentRepository.findAllByActiveTrue().stream()
                .sorted(java.util.Comparator.comparing(item -> item.getRoute().getDriver().getId())).toList()) {
            driverRepository.lockById(enrollment.getRoute().getDriver().getId());
            if (!eligibility.eligible(enrollment.getStudent(), enrollment.getRoute())
                    || !enrollment.getStudent().isProfileComplete()) continue;
            for (RecurringRouteSchedule schedule : enrollment.getRoute().getSchedules()) {
                if (schedule.getDayOfWeek() != serviceDate.getDayOfWeek()
                        || (schedule.getDirection() == Direction.IDA && !enrollment.isOutboundEnabled())
                        || (schedule.getDirection() == Direction.VOLTA && !enrollment.isReturnEnabled())) continue;
                if (release(enrollment, schedule, serviceDate, now)) created++;
            }
        }
        return created;
    }

    @Transactional
    public int expirePending() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<DailyConfirmation> expired = confirmationRepository
                .findAllByStatusAndResponseDeadlineLessThanEqual(DailyConfirmationStatus.PENDING, now);
        expired.forEach(confirmation -> confirmation.setStatus(DailyConfirmationStatus.NO_RESPONSE));
        return expired.size();
    }

    @Transactional
    public DailyConfirmationResponse answer(
            @NotNull Long studentId,
            @NotNull Long confirmationId,
            @NotNull DailyConfirmationStatus answer) {
        if (answer != DailyConfirmationStatus.YES && answer != DailyConfirmationStatus.NO) {
            throw new BusinessRuleException("A resposta deve ser YES ou NO");
        }
        DailyConfirmation confirmation = confirmationRepository.findByIdAndStudentId(confirmationId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Confirmação", confirmationId));
        driverRepository.lockById(confirmation.getDriver().getId());
        if (!eligibility.eligible(confirmation)) throw new BusinessRuleException("A inscrição, instituição ou vínculo não permite participar desta rota");
        var occurrence = tripRepository.findByRecurringRouteIdAndServiceDateAndDirection(
                confirmation.getRecurringRoute().getId(), confirmation.getServiceDate(), confirmation.getDirection());
        if (occurrence.filter(trip -> trip.getStartedAt() != null || trip.getStatus() == TripStatus.CANCELLED).isPresent()) {
            throw new BusinessRuleException("A lista desta viagem já foi encerrada");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isBefore(confirmation.getAvailableAt())) {
            throw new BusinessRuleException("A confirmação ainda não está disponível");
        }
        if (!now.isBefore(confirmation.getResponseDeadline())) {
            throw new BusinessRuleException("O prazo para responder esta confirmação terminou");
        }
        if (confirmation.getStatus() == answer) {
            return DailyConfirmationResponse.from(confirmation);
        }
        if (confirmation.getStatus() != DailyConfirmationStatus.PENDING) {
            throw new BusinessRuleException("A resposta da confirmação não pode ser alterada");
        }

        confirmation.setStatus(answer);
        confirmation.setRespondedAt(now);
        notificationService.create(
                confirmation.getDriver().getId(),
                NotificationType.CONFIRMATION_RECEIVED,
                "Resposta de " + confirmation.getStudent().getUser().getName(),
                answer == DailyConfirmationStatus.YES
                        ? "O aluno confirmou que participará da viagem"
                        : "O aluno informou que não participará da viagem",
                null,
                confirmation);
        return DailyConfirmationResponse.from(confirmation);
    }

    @Transactional(readOnly = true)
    public List<DailyConfirmationResponse> findByStudent(
            @NotNull Long studentId, @NotNull LocalDate serviceDate) {
        return confirmationRepository.findAllByStudentIdAndServiceDateOrderByScheduledTime(studentId, serviceDate)
                .stream().map(DailyConfirmationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<DailyConfirmationResponse> findByDriver(
            @NotNull Long driverId, @NotNull LocalDate serviceDate) {
        return confirmationRepository.findAllByDriverIdAndServiceDateOrderByScheduledTime(driverId, serviceDate)
                .stream().map(DailyConfirmationResponse::from).toList();
    }

    private boolean release(
            RecurringRouteEnrollment enrollment, RecurringRouteSchedule schedule,
            LocalDate serviceDate, LocalDateTime now) {
        if (confirmationRepository.existsByStudentIdAndServiceDateAndDirectionAndRecurringRouteId(
                enrollment.getStudent().getId(), serviceDate, schedule.getDirection(), enrollment.getRoute().getId())) return false;
        var academic = enrollment.getStudent().getSchedules().stream()
                .filter(item -> item.getDayOfWeek() == serviceDate.getDayOfWeek() && item.getDirection() == schedule.getDirection())
                .findFirst().orElse(null);
        if (academic == null) return false;
        if (tripRepository.findByRecurringRouteIdAndServiceDateAndDirection(enrollment.getRoute().getId(), serviceDate, schedule.getDirection())
                .filter(trip -> trip.getStartedAt() != null || trip.getStatus() == TripStatus.CANCELLED).isPresent()) return false;
        LocalDateTime departure = serviceDate.atTime(schedule.getDepartureTime());
        LocalDateTime deadline = routeDeadline(schedule, serviceDate);
        LocalDateTime availableAt = availability(departure, serviceDate);
        if (now.isBefore(availableAt)) return false;
        DailyConfirmationStatus initialStatus = now.isBefore(deadline)
                ? DailyConfirmationStatus.PENDING : DailyConfirmationStatus.NO_RESPONSE;
        DailyConfirmation confirmation = confirmationRepository.save(DailyConfirmation.builder()
                .driver(enrollment.getRoute().getDriver()).recurringRoute(enrollment.getRoute())
                .student(enrollment.getStudent()).serviceDate(serviceDate).direction(schedule.getDirection())
                .scheduledTime(schedule.getDepartureTime()).academicTime(academic.getTime()).preliminaryDepartureAt(departure)
                .availableAt(availableAt).responseDeadline(deadline).status(initialStatus).createdAt(now).build());
        if (initialStatus == DailyConfirmationStatus.PENDING) {
            notificationService.create(enrollment.getStudent().getId(), NotificationType.DAILY_CONFIRMATION_REQUESTED,
                    "Confirme sua viagem", "Você utilizará o transporte na " + schedule.getDirection().name().toLowerCase()
                            + " de " + serviceDate + "?", null, confirmation);
        }
        return true;
    }

    private LocalDateTime routeDeadline(RecurringRouteSchedule schedule, LocalDate serviceDate) {
        LocalDateTime departure = serviceDate.atTime(schedule.getDepartureTime());
        LocalDateTime deadline = serviceDate.atTime(schedule.getResponseDeadlineTime());
        return deadline.isBefore(departure) ? deadline : deadline.minusDays(1);
    }

    private LocalDateTime availability(LocalDateTime departure, LocalDate serviceDate) {
        boolean early = departure.toLocalDate().isBefore(serviceDate)
                || departure.toLocalTime().isBefore(properties.getEarlyDepartureThreshold());
        return early
                ? serviceDate.minusDays(1).atTime(properties.getPreviousDayConfirmationTime())
                : serviceDate.atTime(properties.getSameDayConfirmationTime());
    }
}
