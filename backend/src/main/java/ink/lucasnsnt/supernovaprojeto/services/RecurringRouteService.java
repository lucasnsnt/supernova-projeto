package ink.lucasnsnt.supernovaprojeto.services;

import ink.lucasnsnt.supernovaprojeto.dtos.route.*;
import ink.lucasnsnt.supernovaprojeto.exceptions.BusinessRuleException;
import ink.lucasnsnt.supernovaprojeto.exceptions.ResourceConflictException;
import ink.lucasnsnt.supernovaprojeto.exceptions.ResourceNotFoundException;
import ink.lucasnsnt.supernovaprojeto.models.*;
import ink.lucasnsnt.supernovaprojeto.models.enums.DriverStudentLinkStatus;
import ink.lucasnsnt.supernovaprojeto.models.enums.RouteEnrollmentStatus;
import ink.lucasnsnt.supernovaprojeto.repositories.*;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecurringRouteService {
    private final RecurringRouteRepository routeRepository;
    private final RecurringRouteEnrollmentRepository enrollmentRepository;
    private final VehicleRepository vehicleRepository;
    private final InstitutionRepository institutionRepository;
    private final StudentRepository studentRepository;
    private final DriverStudentLinkRepository linkRepository;
    private final DriverService driverService;
    private final Clock clock;

    @Transactional
    public RecurringRouteResponse create(@NotNull Long driverId, RecurringRouteCreateRequest request) {
        Driver driver = driverService.requireApproved(driverId);
        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .filter(found -> found.getDriver().getId().equals(driverId))
                .orElseThrow(() -> new ResourceNotFoundException("Veículo", request.vehicleId()));
        ensureUniqueSchedules(request);
        ensureUniqueInstitutions(request);
        RecurringRoute route = RecurringRoute.builder().driver(driver).vehicle(vehicle)
                .name(request.name().trim()).createdAt(LocalDateTime.now(clock)).build();
        request.schedules().forEach(item -> route.addSchedule(RecurringRouteSchedule.builder()
                .dayOfWeek(item.dayOfWeek()).direction(item.direction()).departureTime(item.departureTime())
                .responseDeadlineTime(item.responseDeadlineTime()).build()));
        request.institutions().forEach(item -> route.addInstitution(RecurringRouteInstitution.builder()
                .institution(institutionRepository.findById(item.institutionId()).orElseThrow(() -> new ResourceNotFoundException("Instituição", item.institutionId())))
                .stopOrder(item.stopOrder()).build()));
        return RecurringRouteResponse.from(routeRepository.save(route));
    }

    @Transactional(readOnly = true)
    public List<RecurringRouteResponse> findByDriver(@NotNull Long driverId) {
        driverService.requireOperationalView(driverId);
        return routeRepository.findAllByDriverIdOrderByName(driverId).stream().map(RecurringRouteResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RecurringRouteResponse> findAvailableForStudent(@NotNull Long studentId) {
        Student student = studentRepository.findById(studentId).orElseThrow(() -> new ResourceNotFoundException("Aluno", studentId));
        if (student.getInstitution() == null) return List.of();
        return routeRepository.findAllByActiveTrueOrderByName().stream()
                .filter(route -> linkRepository.findFirstByStudentIdAndStatus(studentId, DriverStudentLinkStatus.ACTIVE)
                        .map(link -> link.getDriver().getId().equals(route.getDriver().getId())).orElse(false))
                .filter(route -> route.getInstitutions().stream().anyMatch(stop -> stop.getInstitution().getId().equals(student.getInstitution().getId())))
                .map(RecurringRouteResponse::from).toList();
    }

    @Transactional
    public RouteEnrollmentResponse requestEnrollment(@NotNull Long studentId, RouteEnrollmentRequest request) {
        Student student = studentRepository.findById(studentId).orElseThrow(() -> new ResourceNotFoundException("Aluno", studentId));
        RecurringRoute route = routeRepository.findById(request.routeId()).orElseThrow(() -> new ResourceNotFoundException("Rota", request.routeId()));
        if (!route.isActive()) throw new BusinessRuleException("Esta rota não está aceitando inscrições");
        ensureStudentCanUseRoute(student, route);
        if (enrollmentRepository.findByRouteIdAndStudentId(route.getId(), studentId).isPresent()) {
            throw new ResourceConflictException("O aluno já possui uma solicitação para esta rota");
        }
        return RouteEnrollmentResponse.from(enrollmentRepository.save(RecurringRouteEnrollment.builder()
                .route(route).student(student).outboundEnabled(request.outboundEnabled()).returnEnabled(request.returnEnabled())
                .status(RouteEnrollmentStatus.PENDING).requestedAt(LocalDateTime.now(clock)).build()));
    }

    @Transactional
    public RouteEnrollmentResponse reviewEnrollment(@NotNull Long driverId, @NotNull Long enrollmentId, RouteEnrollmentReviewRequest request) {
        driverService.requireApproved(driverId);
        if (request.status() == RouteEnrollmentStatus.PENDING) throw new BusinessRuleException("Escolha aprovar ou rejeitar a solicitação");
        RecurringRouteEnrollment enrollment = enrollmentRepository.findById(enrollmentId).orElseThrow(() -> new ResourceNotFoundException("Inscrição", enrollmentId));
        if (!enrollment.getRoute().getDriver().getId().equals(driverId)) throw new ResourceNotFoundException("Inscrição", enrollmentId);
        enrollment.setStatus(request.status());
        enrollment.setReviewedAt(LocalDateTime.now(clock));
        return RouteEnrollmentResponse.from(enrollment);
    }

    @Transactional(readOnly = true)
    public List<RouteEnrollmentResponse> findEnrollmentsForDriver(@NotNull Long driverId, @NotNull Long routeId) {
        driverService.requireOperationalView(driverId);
        routeRepository.findByIdAndDriverId(routeId, driverId).orElseThrow(() -> new ResourceNotFoundException("Rota", routeId));
        return enrollmentRepository.findAllByRouteIdOrderByRequestedAtDesc(routeId).stream().map(RouteEnrollmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RouteEnrollmentResponse> findEnrollmentsForStudent(@NotNull Long studentId) {
        return enrollmentRepository.findAllByStudentIdOrderByRequestedAtDesc(studentId).stream().map(RouteEnrollmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RecurringRoutePreviewResponse> findPreviewsForDriver(@NotNull Long driverId, LocalDate date) {
        driverService.requireOperationalView(driverId);
        return previewsFor(date, enrollmentRepository.findAllByStatus(RouteEnrollmentStatus.APPROVED).stream()
                .filter(item -> item.getRoute().getDriver().getId().equals(driverId)).toList());
    }

    @Transactional(readOnly = true)
    public List<RecurringRoutePreviewResponse> findPreviewsForStudent(@NotNull Long studentId, LocalDate date) {
        return previewsFor(date, enrollmentRepository.findAllByStatus(RouteEnrollmentStatus.APPROVED).stream()
                .filter(item -> item.getStudent().getId().equals(studentId)).toList());
    }

    private List<RecurringRoutePreviewResponse> previewsFor(LocalDate date, List<RecurringRouteEnrollment> enrollments) {
        return List.of(ink.lucasnsnt.supernovaprojeto.models.enums.Direction.IDA, ink.lucasnsnt.supernovaprojeto.models.enums.Direction.VOLTA).stream()
                .flatMap(direction -> enrollments.stream().filter(item -> direction == ink.lucasnsnt.supernovaprojeto.models.enums.Direction.IDA ? item.isOutboundEnabled() : item.isReturnEnabled())
                        .collect(java.util.stream.Collectors.groupingBy(item -> item.getRoute().getId())).values().stream()
                        .flatMap(group -> group.getFirst().getRoute().getSchedules().stream()
                                .filter(schedule -> schedule.getDayOfWeek().equals(date.getDayOfWeek()) && schedule.getDirection() == direction)
                                .map(schedule -> RecurringRoutePreviewResponse.from(date, direction, schedule.getDepartureTime(), group))))
                .toList();
    }

    private void ensureStudentCanUseRoute(Student student, RecurringRoute route) {
        boolean linked = linkRepository.findFirstByStudentIdAndStatus(student.getId(), DriverStudentLinkStatus.ACTIVE)
                .map(link -> link.getDriver().getId().equals(route.getDriver().getId())).orElse(false);
        if (!linked) throw new BusinessRuleException("A rota só está disponível para alunos vinculados ao motorista");
        if (student.getInstitution() == null || route.getInstitutions().stream().noneMatch(stop -> stop.getInstitution().getId().equals(student.getInstitution().getId()))) {
            throw new BusinessRuleException("A rota não atende a instituição do aluno");
        }
    }

    private void ensureUniqueSchedules(RecurringRouteCreateRequest request) {
        Set<String> keys = new HashSet<>();
        Set<String> daysWithSchedules = new HashSet<>();
        for (var schedule : request.schedules()) {
            String day = schedule.dayOfWeek().toString();
            if (!keys.add(day + ":" + schedule.direction())) {
                throw new BusinessRuleException("Cada rota pode ter somente uma ida e uma volta por dia");
            }
            daysWithSchedules.add(day);
        }
        for (String day : daysWithSchedules) {
            if (!keys.contains(day + ":IDA") || !keys.contains(day + ":VOLTA")) {
                throw new BusinessRuleException("Cada dia configurado precisa ter uma ida e uma volta");
            }
        }
    }

    private void ensureUniqueInstitutions(RecurringRouteCreateRequest request) {
        Set<Long> institutions = new HashSet<>(); Set<Integer> orders = new HashSet<>();
        for (var stop : request.institutions()) {
            if (!institutions.add(stop.institutionId()) || !orders.add(stop.stopOrder())) {
                throw new BusinessRuleException("Não repita instituições ou ordem de parada na mesma rota");
            }
        }
    }
}
