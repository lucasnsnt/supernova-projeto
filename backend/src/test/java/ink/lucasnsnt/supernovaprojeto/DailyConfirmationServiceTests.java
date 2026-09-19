package ink.lucasnsnt.supernovaprojeto;

import ink.lucasnsnt.supernovaprojeto.config.DailyTransportProperties;
import ink.lucasnsnt.supernovaprojeto.exceptions.BusinessRuleException;
import ink.lucasnsnt.supernovaprojeto.models.*;
import ink.lucasnsnt.supernovaprojeto.models.enums.*;
import ink.lucasnsnt.supernovaprojeto.repositories.DailyConfirmationRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.DriverStudentLinkRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.RecurringRouteEnrollmentRepository;
import ink.lucasnsnt.supernovaprojeto.services.DailyConfirmationService;
import ink.lucasnsnt.supernovaprojeto.services.InAppNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DailyConfirmationServiceTests {

    private DailyConfirmationRepository confirmationRepository;
    private DriverStudentLinkRepository linkRepository;
    private RecurringRouteEnrollmentRepository routeEnrollmentRepository;
    private InAppNotificationService notificationService;
    private DailyConfirmationService service;
    private final ink.lucasnsnt.supernovaprojeto.services.RouteEligibilityService eligibility = mock(ink.lucasnsnt.supernovaprojeto.services.RouteEligibilityService.class);
    private final ink.lucasnsnt.supernovaprojeto.repositories.DriverRepository drivers = mock(ink.lucasnsnt.supernovaprojeto.repositories.DriverRepository.class);
    private final ink.lucasnsnt.supernovaprojeto.repositories.TripRepository trips = mock(ink.lucasnsnt.supernovaprojeto.repositories.TripRepository.class);

    @BeforeEach
    void setUp() {
        when(eligibility.eligible(any(Student.class), any(RecurringRoute.class))).thenReturn(true);
        when(eligibility.eligible(any(DailyConfirmation.class))).thenReturn(true);
        confirmationRepository = mock(DailyConfirmationRepository.class);
        linkRepository = mock(DriverStudentLinkRepository.class);
        routeEnrollmentRepository = mock(RecurringRouteEnrollmentRepository.class);
        notificationService = mock(InAppNotificationService.class);
        DailyTransportProperties properties = new DailyTransportProperties();
        Clock clock = Clock.fixed(
                Instant.parse("2026-09-14T23:00:00Z"), ZoneId.of("America/Bahia"));
        service = new DailyConfirmationService(
                confirmationRepository, linkRepository, routeEnrollmentRepository, notificationService, properties, clock, drivers, trips, eligibility);
    }

    @Test
    void shouldNotReleaseTransportFromAcademicScheduleAlone() {
        LocalDate serviceDate = LocalDate.of(2026, 9, 15);
        when(linkRepository.findAllByStatus(DriverStudentLinkStatus.ACTIVE))
                .thenReturn(List.of(eligibleLink(serviceDate.getDayOfWeek(), LocalTime.of(7, 0))));
        assertThat(service.releaseAvailableForDate(serviceDate)).isZero();
        verify(confirmationRepository, never()).save(any());
    }

    @Test
    void shouldUseDriverRouteTimesForAnActiveMembership() {
        LocalDate serviceDate = LocalDate.of(2026, 9, 15);
        DriverStudentLink link = eligibleLink(serviceDate.getDayOfWeek(), LocalTime.of(18, 30));
        Vehicle vehicle = Vehicle.builder().model("Van").licensePlate("ABC1D23").passengerCapacity(15).driver(link.getDriver()).build();
        RecurringRoute route = RecurringRoute.builder().id(80L).driver(link.getDriver()).vehicle(vehicle).name("Noturna").createdAt(LocalDateTime.now()).build();
        route.addSchedule(RecurringRouteSchedule.builder().dayOfWeek(serviceDate.getDayOfWeek()).direction(Direction.IDA)
                .departureTime(LocalTime.of(17, 10)).responseDeadlineTime(LocalTime.of(16, 10)).build());
        RecurringRouteEnrollment enrollment = RecurringRouteEnrollment.builder().route(route).student(link.getStudent())
                .outboundEnabled(true).returnEnabled(false).active(true).requestedAt(LocalDateTime.now()).build();
        when(routeEnrollmentRepository.findAllByActiveTrue()).thenReturn(List.of(enrollment));
        when(linkRepository.findAllByStatus(DriverStudentLinkStatus.ACTIVE)).thenReturn(List.of(link));
        when(confirmationRepository.existsByStudentIdAndServiceDateAndDirectionAndScheduledTime(
                20L, serviceDate, Direction.IDA, LocalTime.of(17, 10))).thenReturn(false);
        when(confirmationRepository.existsByStudentIdAndServiceDateAndDirection(
                20L, serviceDate, Direction.IDA)).thenReturn(true);
        when(confirmationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DailyConfirmationService routeService = new DailyConfirmationService(
                confirmationRepository, linkRepository, routeEnrollmentRepository, notificationService,
                new DailyTransportProperties(), Clock.fixed(Instant.parse("2026-09-15T15:00:00Z"), ZoneId.of("America/Bahia")), drivers, trips, eligibility);
        routeService.releaseAvailableForDate(serviceDate);

        var captor = org.mockito.ArgumentCaptor.forClass(DailyConfirmation.class);
        verify(confirmationRepository).save(captor.capture());
        assertThat(captor.getValue().getRecurringRoute().getId()).isEqualTo(80L);
        assertThat(captor.getValue().getPreliminaryDepartureAt()).isEqualTo("2026-09-15T17:10:00");
        assertThat(captor.getValue().getResponseDeadline()).isEqualTo("2026-09-15T16:10:00");
        assertThat(captor.getValue().getAcademicTime()).isEqualTo(LocalTime.of(18, 30));
    }

    @Test
    void shouldReleaseMoreThanOneRouteTimeInTheSameDirection() {
        LocalDate serviceDate = LocalDate.of(2026, 9, 15);
        DriverStudentLink link = eligibleLink(serviceDate.getDayOfWeek(), LocalTime.of(18, 30));
        Vehicle vehicle = Vehicle.builder().model("Van").licensePlate("ABC1D23").passengerCapacity(15).driver(link.getDriver()).build();
        RecurringRoute route = RecurringRoute.builder().id(80L).driver(link.getDriver()).vehicle(vehicle).name("Noturna").createdAt(LocalDateTime.now()).build();
        route.addSchedule(RecurringRouteSchedule.builder().dayOfWeek(serviceDate.getDayOfWeek()).direction(Direction.IDA)
                .departureTime(LocalTime.of(17, 10)).responseDeadlineTime(LocalTime.of(16, 10)).build());
        route.addSchedule(RecurringRouteSchedule.builder().dayOfWeek(serviceDate.getDayOfWeek()).direction(Direction.IDA)
                .departureTime(LocalTime.of(19, 10)).responseDeadlineTime(LocalTime.of(18, 10)).build());
        RecurringRouteEnrollment enrollment = RecurringRouteEnrollment.builder().route(route).student(link.getStudent())
                .outboundEnabled(true).returnEnabled(false).active(true).requestedAt(LocalDateTime.now()).build();
        when(routeEnrollmentRepository.findAllByActiveTrue()).thenReturn(List.of(enrollment));
        when(confirmationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DailyConfirmationService routeService = new DailyConfirmationService(
                confirmationRepository, linkRepository, routeEnrollmentRepository, notificationService,
                new DailyTransportProperties(), Clock.fixed(Instant.parse("2026-09-15T15:00:00Z"), ZoneId.of("America/Bahia")), drivers, trips, eligibility);

        assertThat(routeService.releaseAvailableForDate(serviceDate)).isEqualTo(2);
        var captor = org.mockito.ArgumentCaptor.forClass(DailyConfirmation.class);
        verify(confirmationRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(DailyConfirmation::getScheduledTime)
                .containsExactly(LocalTime.of(17, 10), LocalTime.of(19, 10));
    }

    @Test
    void shouldKeepFirstAnswerImmutable() {
        DailyConfirmation confirmation = confirmation(LocalDateTime.of(2026, 9, 14, 19, 0),
                LocalDateTime.of(2026, 9, 14, 21, 0));
        when(confirmationRepository.findByIdAndStudentId(30L, 20L))
                .thenReturn(Optional.of(confirmation));

        var response = service.answer(20L, 30L, DailyConfirmationStatus.YES);

        assertThat(response.status()).isEqualTo(DailyConfirmationStatus.YES);
        assertThat(confirmation.getRespondedAt()).isEqualTo("2026-09-14T20:00:00");
        assertThatThrownBy(() -> service.answer(20L, 30L, DailyConfirmationStatus.NO))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("não pode ser alterada");
    }

    private DriverStudentLink eligibleLink(DayOfWeek dayOfWeek, LocalTime time) {
        User driverUser = User.builder().id(10L).name("Motorista").build();
        Driver driver = Driver.builder().user(driverUser).status(DriverStatus.APPROVED).build();
        driver.setId(10L);
        User studentUser = User.builder().id(20L).name("Aluno").address(address()).build();
        Student student = Student.builder()
                .user(studentUser)
                .institution(Institution.builder().name("Universidade").address(address()).build())
                .build();
        student.setId(20L);
        student.addSchedule(StudentSchedule.builder()
                .dayOfWeek(dayOfWeek).time(time).direction(Direction.IDA).build());
        return DriverStudentLink.builder()
                .driver(driver).student(student).status(DriverStudentLinkStatus.ACTIVE).build();
    }

    private DailyConfirmation confirmation(LocalDateTime availableAt, LocalDateTime deadline) {
        DriverStudentLink link = eligibleLink(DayOfWeek.MONDAY, LocalTime.of(7, 0));
        return DailyConfirmation.builder()
                .id(30L)
                .driver(link.getDriver())
                .recurringRoute(RecurringRoute.builder().id(80L).driver(link.getDriver()).build())
                .student(link.getStudent())
                .serviceDate(LocalDate.of(2026, 9, 15))
                .direction(Direction.IDA)
                .scheduledTime(LocalTime.of(7, 0))
                .preliminaryDepartureAt(LocalDateTime.of(2026, 9, 15, 6, 0))
                .availableAt(availableAt)
                .responseDeadline(deadline)
                .status(DailyConfirmationStatus.PENDING)
                .createdAt(availableAt)
                .build();
    }

    private Address address() {
        return Address.builder()
                .street("Rua").number("1").neighborhood("Centro")
                .city("Salvador").state("BA").zipCode("40000-000").build();
    }
}
