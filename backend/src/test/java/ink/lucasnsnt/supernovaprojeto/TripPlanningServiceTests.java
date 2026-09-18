package ink.lucasnsnt.supernovaprojeto;

import ink.lucasnsnt.supernovaprojeto.config.DailyTransportProperties;
import ink.lucasnsnt.supernovaprojeto.models.*;
import ink.lucasnsnt.supernovaprojeto.models.enums.*;
import ink.lucasnsnt.supernovaprojeto.repositories.*;
import ink.lucasnsnt.supernovaprojeto.services.InAppNotificationService;
import ink.lucasnsnt.supernovaprojeto.services.GeocodingService;
import ink.lucasnsnt.supernovaprojeto.services.DriverService;
import ink.lucasnsnt.supernovaprojeto.exceptions.BusinessRuleException;
import ink.lucasnsnt.supernovaprojeto.services.TripPlanningService;
import ink.lucasnsnt.supernovaprojeto.services.routing.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TripPlanningServiceTests {

    private DailyConfirmationRepository confirmationRepository;
    private TripParticipantRepository participantRepository;
    private TripRepository tripRepository;
    private VehicleRepository vehicleRepository;
    private RoutePlanningGateway routeGateway;
    private InAppNotificationService notificationService;
    private TripPlanningService service;
    private GeocodingService geocodingService;
    private DriverService driverService;
    private final ink.lucasnsnt.supernovaprojeto.services.RouteEligibilityService eligibility = mock(ink.lucasnsnt.supernovaprojeto.services.RouteEligibilityService.class);

    @BeforeEach
    void setUp() {
        confirmationRepository = mock(DailyConfirmationRepository.class);
        participantRepository = mock(TripParticipantRepository.class);
        tripRepository = mock(TripRepository.class);
        vehicleRepository = mock(VehicleRepository.class);
        routeGateway = mock(RoutePlanningGateway.class);
        notificationService = mock(InAppNotificationService.class);
        geocodingService = mock(GeocodingService.class);
        driverService = mock(DriverService.class);
        when(eligibility.eligible(any(DailyConfirmation.class))).thenAnswer(invocation ->
                ((DailyConfirmation) invocation.getArgument(0)).getDriver().getStatus() == DriverStatus.APPROVED);
        when(confirmationRepository.findAllByRecurringRouteIdAndServiceDateAndDirection(anyLong(), any(), any()))
                .thenAnswer(invocation -> confirmationRepository.findAllByStatusAndResponseDeadlineLessThanEqualOrderByResponseDeadline(DailyConfirmationStatus.YES, LocalDateTime.now()));
        ZoneId zone = ZoneId.of("America/Bahia");
        Clock clock = Clock.fixed(
                LocalDateTime.of(2026, 9, 15, 11, 30).atZone(zone).toInstant(), zone);
        service = new TripPlanningService(
                confirmationRepository, participantRepository, tripRepository, vehicleRepository,
                routeGateway, notificationService, new DailyTransportProperties(), clock,
                geocodingService, driverService, mock(DriverRepository.class), eligibility);
    }

    @Test
    void shouldKeepOneFixedDriverOccurrenceWithIndividualAcademicTimes() {
        DailyConfirmation first = confirmation(101L, 20L, LocalTime.of(12, 0), true);
        DailyConfirmation second = confirmation(102L, 21L, LocalTime.of(12, 20), true);
        DailyConfirmation third = confirmation(103L, 22L, LocalTime.of(13, 0), true);
        when(confirmationRepository
                .findAllByStatusAndResponseDeadlineLessThanEqualOrderByResponseDeadline(
                        eq(DailyConfirmationStatus.YES), any()))
                .thenReturn(List.of(first, second, third));
        when(vehicleRepository.findFirstByDriverIdAndDefaultVehicleTrue(10L))
                .thenReturn(Optional.of(vehicle(first.getDriver(), 15)));
        when(routeGateway.optimize(any())).thenAnswer(invocation -> successful(invocation.getArgument(0)));
        AtomicLong tripIds = new AtomicLong(40);
        when(tripRepository.save(any())).thenAnswer(invocation -> {
            Trip trip = invocation.getArgument(0);
            trip.setId(tripIds.getAndIncrement());
            return trip;
        });

        int created = service.planReadyConfirmations();

        assertThat(created).isOne();
        var requestCaptor = org.mockito.ArgumentCaptor.forClass(RoutePlanningRequest.class);
        verify(routeGateway).optimize(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues())
                .extracting(request -> request.passengers().size())
                .containsExactly(3);
        verify(tripRepository).save(any(Trip.class));
        verify(notificationService, times(3)).create(anyLong(), eq(NotificationType.TRIP_PLANNED),
                anyString(), anyString(), any(Trip.class), any(DailyConfirmation.class));
    }

    @Test
    void shouldKeepConfirmedStudentWhenCoordinatesAreMissing() {
        DailyConfirmation confirmation = confirmation(101L, 20L, LocalTime.of(12, 0), false);
        when(confirmationRepository
                .findAllByStatusAndResponseDeadlineLessThanEqualOrderByResponseDeadline(
                        eq(DailyConfirmationStatus.YES), any()))
                .thenReturn(List.of(confirmation));
        when(vehicleRepository.findFirstByDriverIdAndDefaultVehicleTrue(10L))
                .thenReturn(Optional.of(vehicle(confirmation.getDriver(), 15)));
        var tripCaptor = org.mockito.ArgumentCaptor.forClass(Trip.class);

        assertThat(service.planReadyConfirmations()).isOne();

        verify(routeGateway, never()).optimize(any());
        verify(tripRepository).save(tripCaptor.capture());
        assertThat(tripCaptor.getValue().getStatus()).isEqualTo(TripStatus.NEEDS_ATTENTION);
        assertThat(tripCaptor.getValue().getParticipants()).hasSize(1);
        verify(notificationService).create(eq(10L), eq(NotificationType.PLANNING_NEEDS_ATTENTION),
                anyString(), contains("coordenadas"), same(tripCaptor.getValue()), isNull());
    }

    @Test
    void shouldReplanTripAfterOperationalIssueIsFixed() {
        DailyConfirmation confirmation = confirmation(101L, 20L, LocalTime.of(7, 0), true);
        Vehicle vehicle = vehicle(confirmation.getDriver(), 15);
        Trip trip = Trip.builder()
                .id(40L)
                .driver(confirmation.getDriver()).recurringRoute(confirmation.getRecurringRoute())
                .serviceDate(confirmation.getServiceDate())
                .direction(Direction.IDA)
                .status(TripStatus.NEEDS_ATTENTION)
                .plannedDepartureAt(confirmation.getPreliminaryDepartureAt())
                .planningIssue("Coordenadas ausentes")
                .createdAt(LocalDateTime.of(2026, 9, 15, 6, 0))
                .build();
        trip.addParticipant(TripParticipant.builder()
                .student(confirmation.getStudent())
                .confirmation(confirmation)
                .pickupOrder(1)
                .dropoffOrder(1)
                .build());
        when(tripRepository.findByIdAndDriverId(40L, 10L)).thenReturn(Optional.of(trip));
        when(vehicleRepository.findFirstByDriverIdAndDefaultVehicleTrue(10L))
                .thenReturn(Optional.of(vehicle));
        when(routeGateway.optimize(any())).thenAnswer(invocation -> successful(invocation.getArgument(0)));

        doReturn(List.of(confirmation)).when(confirmationRepository)
                .findAllByRecurringRouteIdAndServiceDateAndDirection(anyLong(), any(), any());
        var response = service.replan(10L, 40L);

        assertThat(response.status()).isEqualTo(TripStatus.PLANNED);
        assertThat(response.planningIssue()).isNull();
        assertThat(response.vehicle().id()).isEqualTo(50L);
        assertThat(trip.getRouteCalculatedAt()).isNotNull();
        verify(routeGateway).optimize(any(RoutePlanningRequest.class));
        verify(notificationService).create(eq(20L), eq(NotificationType.TRIP_PLANNED),
                anyString(), anyString(), same(trip), same(confirmation));
    }

    @Test
    void shouldNotPlanConfirmationsOfSuspendedDriver() {
        DailyConfirmation confirmation = confirmation(101L, 20L, LocalTime.of(12, 0), true);
        confirmation.getDriver().setStatus(DriverStatus.SUSPENDED);
        when(confirmationRepository.findAllByStatusAndResponseDeadlineLessThanEqualOrderByResponseDeadline(
                eq(DailyConfirmationStatus.YES), any())).thenReturn(List.of(confirmation));
        assertThat(service.planReadyConfirmations()).isZero();
        verifyNoInteractions(routeGateway, tripRepository);
    }

    @Test
    void shouldRequireApprovalBeforeReplanning() {
        doThrow(new BusinessRuleException("Motorista suspenso")).when(driverService).requireApproved(10L);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.replan(10L, 40L))
                .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(tripRepository, routeGateway);
    }

    @Test
    void shouldRetainConfirmedStudentWhenGeocodingFails() {
        DailyConfirmation confirmation = confirmation(101L, 20L, LocalTime.of(12, 0), false);
        when(confirmationRepository.findAllByStatusAndResponseDeadlineLessThanEqualOrderByResponseDeadline(
                eq(DailyConfirmationStatus.YES), any())).thenReturn(List.of(confirmation));
        when(vehicleRepository.findFirstByDriverIdAndDefaultVehicleTrue(10L))
                .thenReturn(Optional.of(vehicle(confirmation.getDriver(), 15)));
        doThrow(new BusinessRuleException("Confira rua, número, cidade e CEP"))
                .when(geocodingService).resolve(any());
        assertThat(service.planReadyConfirmations()).isOne();
        var captor = org.mockito.ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TripStatus.NEEDS_ATTENTION);
        assertThat(captor.getValue().getParticipants()).hasSize(1);
        assertThat(captor.getValue().getPlanningIssue()).contains("Confira rua");
        verifyNoInteractions(routeGateway);
    }

    private RoutePlanningResult successful(RoutePlanningRequest request) {
        int[] order = {1};
        List<RouteStopPlan> stops = request.passengers().stream()
                .map(passenger -> {
                    int current = order[0]++;
                    return new RouteStopPlan(passenger.confirmationId(), current, current,
                            LocalDateTime.of(2026, 9, 15, 12, current),
                            LocalDateTime.of(2026, 9, 15, 12, current + 10));
                })
                .toList();
        return new RoutePlanningResult(true, "TEST", "route-test",
                LocalDateTime.of(2026, 9, 15, 11, 45), "polyline", null, stops);
    }

    private DailyConfirmation confirmation(
            Long confirmationId, Long studentId, LocalTime time, boolean coordinates) {
        Address base = address(coordinates);
        User driverUser = User.builder().id(10L).name("Motorista").address(base).build();
        Driver driver = Driver.builder().user(driverUser).status(DriverStatus.APPROVED).build();
        driver.setId(10L);
        User studentUser = User.builder().id(studentId).name("Aluno " + studentId)
                .address(address(coordinates)).build();
        Institution institution = Institution.builder().id(60L).name("Universidade")
                .address(address(coordinates)).build();
        Student student = Student.builder().user(studentUser).institution(institution).build();
        student.setId(studentId);
        return DailyConfirmation.builder()
                .id(confirmationId).driver(driver).student(student)
                .recurringRoute(RecurringRoute.builder().id(80L).driver(driver).vehicle(vehicle(driver, 15)).name("Rota").build())
                .serviceDate(LocalDate.of(2026, 9, 15)).direction(Direction.VOLTA)
                .scheduledTime(LocalTime.NOON).academicTime(time).preliminaryDepartureAt(LocalDate.of(2026, 9, 15).atTime(LocalTime.NOON))
                .availableAt(LocalDateTime.of(2026, 9, 15, 6, 0))
                .responseDeadline(LocalDateTime.of(2026, 9, 15, 11, 0))
                .status(DailyConfirmationStatus.YES).createdAt(LocalDateTime.of(2026, 9, 15, 6, 0))
                .build();
    }

    private Vehicle vehicle(Driver driver, int capacity) {
        return Vehicle.builder().id(50L).driver(driver).brand("Fiat").model("Ducato")
                .licensePlate("ABC1D23").passengerCapacity(capacity).defaultVehicle(true).build();
    }

    private Address address(boolean coordinates) {
        return Address.builder()
                .street("Rua").number("1").neighborhood("Centro")
                .city("Salvador").state("BA").zipCode("40000-000")
                .latitude(coordinates ? new BigDecimal("-12.9714") : null)
                .longitude(coordinates ? new BigDecimal("-38.5014") : null)
                .build();
    }
}
