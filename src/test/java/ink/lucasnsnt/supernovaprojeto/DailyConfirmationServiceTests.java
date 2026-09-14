package ink.lucasnsnt.supernovaprojeto;

import ink.lucasnsnt.supernovaprojeto.config.DailyTransportProperties;
import ink.lucasnsnt.supernovaprojeto.exceptions.BusinessRuleException;
import ink.lucasnsnt.supernovaprojeto.models.*;
import ink.lucasnsnt.supernovaprojeto.models.enums.*;
import ink.lucasnsnt.supernovaprojeto.repositories.DailyConfirmationRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.DriverStudentLinkRepository;
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
    private InAppNotificationService notificationService;
    private DailyConfirmationService service;

    @BeforeEach
    void setUp() {
        confirmationRepository = mock(DailyConfirmationRepository.class);
        linkRepository = mock(DriverStudentLinkRepository.class);
        notificationService = mock(InAppNotificationService.class);
        DailyTransportProperties properties = new DailyTransportProperties();
        Clock clock = Clock.fixed(
                Instant.parse("2026-09-14T23:00:00Z"), ZoneId.of("America/Bahia"));
        service = new DailyConfirmationService(
                confirmationRepository, linkRepository, notificationService, properties, clock);
    }

    @Test
    void shouldReleaseEarlyConfirmationOnPreviousEvening() {
        LocalDate serviceDate = LocalDate.of(2026, 9, 15);
        DriverStudentLink link = eligibleLink(serviceDate.getDayOfWeek(), LocalTime.of(7, 0));
        when(linkRepository.findAllByStatus(DriverStudentLinkStatus.ACTIVE)).thenReturn(List.of(link));
        when(confirmationRepository.existsByStudentIdAndServiceDateAndDirection(
                20L, serviceDate, Direction.IDA)).thenReturn(false);
        when(confirmationRepository.save(any())).thenAnswer(invocation -> {
            DailyConfirmation confirmation = invocation.getArgument(0);
            confirmation.setId(30L);
            return confirmation;
        });

        int created = service.releaseAvailableForDate(serviceDate);

        assertThat(created).isOne();
        var confirmationCaptor = org.mockito.ArgumentCaptor.forClass(DailyConfirmation.class);
        verify(confirmationRepository).save(confirmationCaptor.capture());
        DailyConfirmation confirmation = confirmationCaptor.getValue();
        assertThat(confirmation.getAvailableAt()).isEqualTo("2026-09-14T20:00:00");
        assertThat(confirmation.getPreliminaryDepartureAt()).isEqualTo("2026-09-15T06:00:00");
        assertThat(confirmation.getResponseDeadline()).isEqualTo("2026-09-15T05:00:00");
        assertThat(confirmation.getStatus()).isEqualTo(DailyConfirmationStatus.PENDING);
        verify(notificationService).create(eq(20L), eq(NotificationType.DAILY_CONFIRMATION_REQUESTED),
                anyString(), anyString(), isNull(), same(confirmation));
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
