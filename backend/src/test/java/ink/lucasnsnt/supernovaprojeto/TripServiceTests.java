package ink.lucasnsnt.supernovaprojeto;

import ink.lucasnsnt.supernovaprojeto.config.DailyTransportProperties;
import ink.lucasnsnt.supernovaprojeto.exceptions.BusinessRuleException;
import ink.lucasnsnt.supernovaprojeto.models.*;
import ink.lucasnsnt.supernovaprojeto.models.enums.*;
import ink.lucasnsnt.supernovaprojeto.repositories.TripRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.VehicleRepository;
import ink.lucasnsnt.supernovaprojeto.services.DriverService;
import ink.lucasnsnt.supernovaprojeto.services.InAppNotificationService;
import ink.lucasnsnt.supernovaprojeto.services.TripService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TripServiceTests {

    private TripRepository tripRepository;
    private VehicleRepository vehicleRepository;
    private DriverService driverService;
    private InAppNotificationService notificationService;
    private TripService service;

    @BeforeEach
    void setUp() {
        tripRepository = mock(TripRepository.class);
        vehicleRepository = mock(VehicleRepository.class);
        driverService = mock(DriverService.class);
        notificationService = mock(InAppNotificationService.class);
        service = serviceAt(LocalDateTime.of(2026, 9, 15, 5, 0));
    }

    @Test
    void shouldAllowDepartureAdjustmentInsideWindow() {
        Trip trip = trip();
        stubOwnedTrip(trip);

        var response = service.updateDeparture(
                10L, 40L, LocalDateTime.of(2026, 9, 15, 6, 30), null);

        assertThat(response.departureAt()).isEqualTo("2026-09-15T06:30:00");
        assertThat(response.departureConfirmedAt()).isEqualTo("2026-09-15T05:00:00");
        verify(notificationService).create(eq(20L), eq(NotificationType.DEPARTURE_TIME_CHANGED),
                anyString(), anyString(), same(trip), any(DailyConfirmation.class));
    }

    @Test
    void shouldRequireReasonAndOnlyAllowDelayAfterLock() {
        Trip trip = trip();
        stubOwnedTrip(trip);
        service = serviceAt(LocalDateTime.of(2026, 9, 15, 5, 45));

        assertThatThrownBy(() -> service.updateDeparture(
                10L, 40L, LocalDateTime.of(2026, 9, 15, 6, 15), null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("motivo");
        assertThatThrownBy(() -> service.updateDeparture(
                10L, 40L, LocalDateTime.of(2026, 9, 15, 5, 55), "Mudança"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("somente atrasos");

        var response = service.updateDeparture(
                10L, 40L, LocalDateTime.of(2026, 9, 15, 6, 15), "Trânsito intenso");
        assertThat(response.departureAt()).isEqualTo("2026-09-15T06:15:00");
    }

    @Test
    void shouldRejectVehicleWithoutPassengerCapacity() {
        Trip trip = trip();
        TripParticipant second = participant(21L);
        trip.addParticipant(second);
        stubOwnedTrip(trip);
        Vehicle smallVehicle = Vehicle.builder()
                .id(51L).driver(trip.getDriver()).brand("Ford").model("Ka")
                .licensePlate("ABC1D23").passengerCapacity(1).build();
        when(vehicleRepository.findById(51L)).thenReturn(Optional.of(smallVehicle));

        assertThatThrownBy(() -> service.changeVehicle(10L, 40L, 51L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("capacidade");
    }

    @Test
    void shouldSendOneReminderInsideDepartureWindow() {
        Trip due = trip();
        Trip future = trip();
        future.setId(41L);
        future.setPlannedDepartureAt(LocalDateTime.of(2026, 9, 15, 7, 0));
        when(tripRepository.findAllByStatusAndDepartureReminderSentAtIsNullAndServiceDate(
                TripStatus.PLANNED, LocalDate.of(2026, 9, 15)))
                .thenReturn(List.of(due, future));
        service = serviceAt(LocalDateTime.of(2026, 9, 15, 5, 30));

        assertThat(service.sendDueDepartureReminders()).isOne();

        assertThat(due.getDepartureReminderSentAt())
                .isEqualTo(LocalDateTime.of(2026, 9, 15, 5, 30));
        assertThat(future.getDepartureReminderSentAt()).isNull();
        verify(notificationService).create(
                eq(10L), eq(NotificationType.DEPARTURE_REMINDER),
                eq("Hora da viagem"), contains("06:00"), same(due), isNull());
    }

    @Test
    void shouldStartAdvanceStopsAndFinishTrip() {
        Trip trip = trip();
        stubOwnedTrip(trip);

        assertThat(service.start(10L, 40L).status()).isEqualTo(TripStatus.IN_PROGRESS);
        assertThat(service.completeNextStop(10L, 40L).completedStopCount()).isOne();
        assertThat(service.complete(10L, 40L).status()).isEqualTo(TripStatus.COMPLETED);
        assertThat(trip.getCompletedAt()).isEqualTo(LocalDateTime.of(2026, 9, 15, 5, 0));
    }

    private TripService serviceAt(LocalDateTime localDateTime) {
        ZoneId zone = ZoneId.of("America/Bahia");
        Clock clock = Clock.fixed(localDateTime.atZone(zone).toInstant(), zone);
        return new TripService(tripRepository, vehicleRepository, driverService,
                notificationService, new DailyTransportProperties(), clock);
    }

    private void stubOwnedTrip(Trip trip) {
        when(tripRepository.findByIdAndDriverId(40L, 10L)).thenReturn(Optional.of(trip));
    }

    private Trip trip() {
        User driverUser = User.builder().id(10L).name("Motorista").build();
        Driver driver = Driver.builder().user(driverUser).status(DriverStatus.APPROVED).build();
        driver.setId(10L);
        Vehicle vehicle = Vehicle.builder()
                .id(50L).driver(driver).brand("Fiat").model("Ducato")
                .licensePlate("DEF4G56").passengerCapacity(15).defaultVehicle(true).build();
        Trip trip = Trip.builder()
                .id(40L).driver(driver).vehicle(vehicle)
                .serviceDate(LocalDate.of(2026, 9, 15)).direction(Direction.IDA)
                .status(TripStatus.PLANNED)
                .plannedDepartureAt(LocalDateTime.of(2026, 9, 15, 6, 0))
                .createdAt(LocalDateTime.of(2026, 9, 14, 20, 0)).build();
        trip.addParticipant(participant(20L));
        return trip;
    }

    private TripParticipant participant(Long studentId) {
        User studentUser = User.builder().id(studentId).name("Aluno " + studentId).build();
        Student student = Student.builder().user(studentUser)
                .institution(Institution.builder().id(60L).name("Universidade").build()).build();
        student.setId(studentId);
        DailyConfirmation confirmation = DailyConfirmation.builder()
                .id(100L + studentId).student(student).status(DailyConfirmationStatus.YES).build();
        return TripParticipant.builder()
                .student(student).confirmation(confirmation)
                .pickupOrder(studentId.intValue()).dropoffOrder(studentId.intValue()).build();
    }
}
