package ink.lucasnsnt.supernovaprojeto;

import ink.lucasnsnt.supernovaprojeto.models.*;
import ink.lucasnsnt.supernovaprojeto.models.enums.Direction;
import ink.lucasnsnt.supernovaprojeto.models.enums.DriverStudentLinkStatus;
import ink.lucasnsnt.supernovaprojeto.models.enums.InstitutionType;
import ink.lucasnsnt.supernovaprojeto.models.enums.Role;
import ink.lucasnsnt.supernovaprojeto.repositories.DriverStudentLinkRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.StudentScheduleRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.UserRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.VehicleRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class JpaRelationshipTests {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private StudentScheduleRepository studentScheduleRepository;

    @Autowired
    private DriverStudentLinkRepository driverStudentLinkRepository;

    @Test
    void shouldPersistAndNavigateMainRelationships() {
        Institution institution = Institution.builder()
                .name("Instituto Supernova")
                .institutionType(InstitutionType.UNIVERSITY)
                .address(address("Avenida Universitaria", "100"))
                .build();
        entityManager.persist(institution);

        User driverUser = user("Motorista", "motorista@supernova.test", Role.DRIVER);
        User studentUser = user("Estudante", "estudante@supernova.test", Role.STUDENT);
        entityManager.persist(driverUser);
        entityManager.persist(studentUser);

        Driver driver = Driver.builder().user(driverUser).cnh("12345678900").build();
        driverUser.setDriver(driver);
        entityManager.persist(driver);

        Student student = Student.builder().user(studentUser).institution(institution).build();
        studentUser.setStudent(student);
        institution.addStudent(student);
        entityManager.persist(student);

        Vehicle vehicle = Vehicle.builder()
                .brand("Volkswagen")
                .model("Kombi")
                .year(2014)
                .licensePlate("ABC1D23")
                .passengerCapacity(12)
                .build();
        driver.addVehicle(vehicle);

        StudentSchedule schedule = StudentSchedule.builder()
                .dayOfWeek(DayOfWeek.MONDAY)
                .time(LocalTime.of(7, 30))
                .direction(Direction.IDA)
                .build();
        student.addSchedule(schedule);

        DriverStudentLink link = DriverStudentLink.builder()
                .startDate(LocalDate.now())
                .status(DriverStudentLinkStatus.ACTIVE)
                .build();
        driver.addStudentLink(link);
        student.addDriverLink(link);

        entityManager.persist(vehicle);
        entityManager.persist(schedule);
        entityManager.persist(link);
        entityManager.flush();
        entityManager.clear();

        Driver persistedDriver = entityManager.find(Driver.class, driver.getId());
        Student persistedStudent = entityManager.find(Student.class, student.getId());

        assertThat(persistedDriver.getUser().getEmail()).isEqualTo("motorista@supernova.test");
        assertThat(persistedDriver.getVehicles()).hasSize(1);
        assertThat(persistedDriver.getStudentLinks().getFirst().getStudent().getId())
                .isEqualTo(persistedStudent.getId());
        assertThat(persistedStudent.getInstitution().getName()).isEqualTo("Instituto Supernova");
        assertThat(persistedStudent.getSchedules()).hasSize(1);
        assertThat(persistedStudent.getDriverLinks().getFirst().getDriver().getId())
                .isEqualTo(persistedDriver.getId());

        assertThat(userRepository.findByEmailIgnoreCase("MOTORISTA@SUPERNOVA.TEST")).isPresent();
        assertThat(vehicleRepository.findAllByDriverId(persistedDriver.getId())).hasSize(1);
        assertThat(studentScheduleRepository.findAllByStudentId(persistedStudent.getId())).hasSize(1);
        assertThat(driverStudentLinkRepository
                .findFirstByDriverIdAndStudentIdAndStatus(
                        persistedDriver.getId(), persistedStudent.getId(), DriverStudentLinkStatus.ACTIVE))
                .isPresent();
    }

    private User user(String name, String email, Role role) {
        return User.builder()
                .name(name)
                .email(email)
                .password("encoded-password")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
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
