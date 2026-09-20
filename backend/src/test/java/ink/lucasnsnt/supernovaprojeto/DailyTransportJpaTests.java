package ink.lucasnsnt.supernovaprojeto;

import ink.lucasnsnt.supernovaprojeto.models.*;
import ink.lucasnsnt.supernovaprojeto.models.enums.*;
import ink.lucasnsnt.supernovaprojeto.repositories.DailyConfirmationRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.InAppNotificationRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.TripRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.TripLocationRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DailyTransportJpaTests {

    @Autowired private EntityManager entityManager;
    @Autowired private DailyConfirmationRepository confirmationRepository;
    @Autowired private TripRepository tripRepository;
    @Autowired private TripLocationRepository locationRepository;
    @Autowired private InAppNotificationRepository notificationRepository;

    @Test
    void shouldPersistDailyTripAggregateAndNotification() {
        User driverUser = user("Motorista Diário", "driver-daily@test.local", Role.DRIVER);
        User studentUser = user("Aluno Diário", "student-daily@test.local", Role.STUDENT);
        entityManager.persist(driverUser);
        entityManager.persist(studentUser);

        Driver driver = Driver.builder()
                .user(driverUser)
                .cnh("CNH-DAILY")
                .status(DriverStatus.APPROVED)
                .operationalAddress(driverUser.getAddress())
                .build();
        driverUser.setDriver(driver);
        entityManager.persist(driver);

        Institution institution = Institution.builder()
                .name("Universidade Diária")
                .institutionType(InstitutionType.UNIVERSITY)
                .address(address("Avenida Acadêmica", "100"))
                .build();
        entityManager.persist(institution);

        Student student = Student.builder().user(studentUser).institution(institution).build();
        studentUser.setStudent(student);
        entityManager.persist(student);

        Vehicle vehicle = Vehicle.builder()
                .driver(driver)
                .brand("Mercedes-Benz")
                .model("Sprinter")
                .licensePlate("DAY1A23")
                .passengerCapacity(15)
                .defaultVehicle(true)
                .build();
        entityManager.persist(vehicle);

        LocalDate serviceDate = LocalDate.of(2026, 9, 15);
        DailyConfirmation confirmation = confirmationRepository.save(DailyConfirmation.builder()
                .driver(driver)
                .student(student)
                .serviceDate(serviceDate)
                .direction(Direction.IDA)
                .scheduledTime(LocalTime.of(7, 0))
                .preliminaryDepartureAt(serviceDate.atTime(6, 0))
                .availableAt(serviceDate.minusDays(1).atTime(20, 0))
                .responseDeadline(serviceDate.atTime(5, 0))
                .status(DailyConfirmationStatus.YES)
                .respondedAt(serviceDate.minusDays(1).atTime(20, 15))
                .createdAt(serviceDate.minusDays(1).atTime(20, 0))
                .build());

        Trip trip = Trip.builder()
                .driver(driver)
                .vehicle(vehicle)
                .serviceDate(serviceDate)
                .direction(Direction.IDA)
                .status(TripStatus.PLANNED)
                .plannedDepartureAt(serviceDate.atTime(6, 0))
                .createdAt(serviceDate.minusDays(1).atTime(20, 0))
                .build();
        trip.addParticipant(TripParticipant.builder()
                .student(student)
                .confirmation(confirmation)
                .pickupOrder(1)
                .dropoffOrder(2)
                .estimatedPickupAt(serviceDate.atTime(6, 15))
                .estimatedDropoffAt(serviceDate.atTime(6, 50))
                .build());
        tripRepository.save(trip);

        notificationRepository.save(InAppNotification.builder()
                .recipient(studentUser)
                .type(NotificationType.DAILY_CONFIRMATION_REQUESTED)
                .title("Confirme sua ida")
                .message("Você utilizará o transporte amanhã?")
                .trip(trip)
                .confirmation(confirmation)
                .createdAt(serviceDate.minusDays(1).atTime(20, 0))
                .build());

        entityManager.flush();
        entityManager.clear();

        Trip persisted = tripRepository.findByIdAndDriverId(trip.getId(), driver.getId()).orElseThrow();
        assertThat(persisted.getVehicle().isDefaultVehicle()).isTrue();
        assertThat(persisted.getParticipants()).hasSize(1);
        assertThat(persisted.getParticipants().getFirst().getConfirmation().getStatus())
                .isEqualTo(DailyConfirmationStatus.YES);
        assertThat(notificationRepository.countByRecipientIdAndReadAtIsNull(student.getId())).isOne();
    }

    @Test
    void shouldPersistFirstLiveLocationWithSharedTripId() {
        User driverUser = user("Motorista GPS", "driver-gps@test.local", Role.DRIVER);
        entityManager.persist(driverUser);
        Driver driver = Driver.builder().user(driverUser).cnh("CNH-GPS")
                .status(DriverStatus.APPROVED).build();
        driverUser.setDriver(driver);
        entityManager.persist(driver);
        Trip trip = Trip.builder().driver(driver).serviceDate(LocalDate.of(2026, 9, 20))
                .direction(Direction.IDA).status(TripStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.of(2026, 9, 20, 16, 0)).build();
        tripRepository.saveAndFlush(trip);

        locationRepository.saveAndFlush(TripLocation.builder().trip(trip).tripId(trip.getId())
                .latitude(-10.91).longitude(-37.07).accuracy(8.0)
                .recordedAt(LocalDateTime.of(2026, 9, 20, 16, 1))
                .updatedAt(LocalDateTime.of(2026, 9, 20, 16, 1)).build());
        entityManager.clear();

        assertThat(locationRepository.findById(trip.getId()))
                .get().extracting(TripLocation::getLatitude, TripLocation::getLongitude)
                .containsExactly(-10.91, -37.07);
    }

    private User user(String name, String email, Role role) {
        return User.builder()
                .name(name)
                .email(email)
                .password("encoded-password")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .emailVerifiedAt(LocalDateTime.now())
                .role(role)
                .address(address("Rua Principal", role.name()))
                .build();
    }

    private Address address(String street, String number) {
        return Address.builder()
                .street(street)
                .number(number)
                .neighborhood("Centro")
                .city("Salvador")
                .state("BA")
                .zipCode("40000-000")
                .build();
    }
}
