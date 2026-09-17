package ink.lucasnsnt.supernovaprojeto;

import ink.lucasnsnt.supernovaprojeto.dtos.trip.TripLocationUpdateRequest;
import ink.lucasnsnt.supernovaprojeto.exceptions.BusinessRuleException;
import ink.lucasnsnt.supernovaprojeto.exceptions.ResourceNotFoundException;
import ink.lucasnsnt.supernovaprojeto.models.*;
import ink.lucasnsnt.supernovaprojeto.models.enums.*;
import ink.lucasnsnt.supernovaprojeto.repositories.TripLocationRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.TripRepository;
import ink.lucasnsnt.supernovaprojeto.services.DriverService;
import ink.lucasnsnt.supernovaprojeto.services.TripTrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TripTrackingServiceTests {
    private TripRepository trips;
    private TripLocationRepository locations;
    private TripTrackingService service;
    private Trip trip;

    @BeforeEach
    void setUp() {
        trips = mock(TripRepository.class);
        locations = mock(TripLocationRepository.class);
        DriverService drivers = mock(DriverService.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-16T12:00:00Z"), ZoneOffset.UTC);
        service = new TripTrackingService(trips, locations, drivers, clock);
        User user = User.builder().id(10L).name("Motorista").build();
        Driver driver = Driver.builder().user(user).status(DriverStatus.APPROVED).build();
        driver.setId(10L);
        trip = Trip.builder().id(40L).driver(driver).serviceDate(LocalDate.of(2026, 9, 16))
                .direction(Direction.IDA).status(TripStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.of(2026, 9, 15, 20, 0)).build();
        when(trips.findByIdAndDriverId(40L, 10L)).thenReturn(Optional.of(trip));
    }

    @Test
    void ownerCanUpdateLocationOnlyWhileTripIsInProgress() {
        when(locations.findById(40L)).thenReturn(Optional.empty());
        when(locations.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateLocation(10L, 40L,
                new TripLocationUpdateRequest(-12.97, -38.50, 8.0, 180.0, null));

        assertThat(response.location().latitude()).isEqualTo(-12.97);
        assertThat(response.location().updatedAt()).isEqualTo("2026-09-16T12:00:00");
        verify(locations).save(any(TripLocation.class));

        trip.setStatus(TripStatus.COMPLETED);
        assertThatThrownBy(() -> service.updateLocation(10L, 40L,
                new TripLocationUpdateRequest(-12.98, -38.51, null, null, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void nonParticipantCannotReadTrackingAndLocationIsHiddenAfterEnd() {
        when(trips.findDistinctByIdAndParticipantsStudentId(40L, 99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findForStudent(99L, 40L))
                .isInstanceOf(ResourceNotFoundException.class);

        TripLocation location = TripLocation.builder().tripId(40L).trip(trip).latitude(-12.97)
                .longitude(-38.50).recordedAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(locations.findById(40L)).thenReturn(Optional.of(location));
        trip.setStatus(TripStatus.CANCELLED);
        assertThat(service.findForDriver(10L, 40L).location()).isNull();
    }
}
