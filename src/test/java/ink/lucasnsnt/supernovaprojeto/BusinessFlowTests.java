package ink.lucasnsnt.supernovaprojeto;

import ink.lucasnsnt.supernovaprojeto.exceptions.BusinessRuleException;
import ink.lucasnsnt.supernovaprojeto.exceptions.ResourceConflictException;
import ink.lucasnsnt.supernovaprojeto.models.Address;
import ink.lucasnsnt.supernovaprojeto.models.Driver;
import ink.lucasnsnt.supernovaprojeto.models.DriverInvite;
import ink.lucasnsnt.supernovaprojeto.models.DriverStudentLink;
import ink.lucasnsnt.supernovaprojeto.models.Institution;
import ink.lucasnsnt.supernovaprojeto.models.Student;
import ink.lucasnsnt.supernovaprojeto.models.User;
import ink.lucasnsnt.supernovaprojeto.models.enums.DriverStatus;
import ink.lucasnsnt.supernovaprojeto.models.enums.DriverStudentLinkStatus;
import ink.lucasnsnt.supernovaprojeto.models.enums.InstitutionType;
import ink.lucasnsnt.supernovaprojeto.models.enums.InviteStatus;
import ink.lucasnsnt.supernovaprojeto.models.enums.Role;
import ink.lucasnsnt.supernovaprojeto.repositories.AddressRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.DriverInviteRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.DriverStudentLinkRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.InstitutionRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.UserRepository;
import ink.lucasnsnt.supernovaprojeto.services.DriverInviteService;
import ink.lucasnsnt.supernovaprojeto.services.DriverService;
import ink.lucasnsnt.supernovaprojeto.services.DriverStudentLinkService;
import ink.lucasnsnt.supernovaprojeto.services.StudentScheduleService;
import ink.lucasnsnt.supernovaprojeto.services.StudentService;
import ink.lucasnsnt.supernovaprojeto.services.VehicleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class BusinessFlowTests {

    @Autowired private UserRepository userRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private InstitutionRepository institutionRepository;
    @Autowired private DriverInviteRepository inviteRepository;
    @Autowired private DriverStudentLinkRepository linkRepository;
    @Autowired private DriverService driverService;
    @Autowired private VehicleService vehicleService;
    @Autowired private DriverInviteService inviteService;
    @Autowired private StudentService studentService;
    @Autowired private StudentScheduleService scheduleService;
    @Autowired private DriverStudentLinkService linkService;

    @Test
    void shouldApplyApprovalInviteProfileAndSuspensionRules() {
        User driverUser = userRepository.save(user("Motorista", "driver@flow.test", Role.DRIVER, address()));
        Driver driver = driverService.register(driverUser.getId(), "99999999999");

        assertThat(driver.getStatus()).isEqualTo(DriverStatus.PENDING);
        assertThatThrownBy(() -> inviteService.create(driver.getId(), false))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> vehicleService.create(
                driver.getId(), "Fiat", "Ducato", 2024, "XYZ1A23", 15, "Branca"))
                .isInstanceOf(BusinessRuleException.class);

        driverService.approve(driver.getId());
        DriverInvite invite = inviteService.create(driver.getId(), false);
        assertThat(invite.getStatus()).isEqualTo(InviteStatus.ACTIVE);
        assertThatThrownBy(() -> inviteService.create(driver.getId(), false))
                .isInstanceOf(ResourceConflictException.class);
        DriverInvite replacementInvite = inviteService.create(driver.getId(), true);
        assertThat(invite.getStatus()).isEqualTo(InviteStatus.REVOKED);
        assertThat(invite.getRevokedAt()).isNotNull();

        User studentUser = userRepository.save(user("Aluno", "student@flow.test", Role.STUDENT, null));
        Student student = studentService.register(studentUser.getId());
        DriverStudentLink link = linkService.acceptInvite(student.getId(), replacementInvite.getToken());

        assertThat(link.getStatus()).isEqualTo(DriverStudentLinkStatus.ACTIVE);
        assertThat(linkService.findStudentsEligibleForRoute(driver.getId())).isEmpty();

        User otherDriverUser = userRepository.save(
                user("Outro Motorista", "other-driver@flow.test", Role.DRIVER, address()));
        Driver otherDriver = driverService.register(otherDriverUser.getId(), "88888888888");
        driverService.approve(otherDriver.getId());
        DriverInvite otherInvite = inviteService.create(otherDriver.getId(), false);
        assertThatThrownBy(() -> linkService.acceptInvite(student.getId(), otherInvite.getToken()))
                .isInstanceOf(ResourceConflictException.class);

        Address studentAddress = addressRepository.save(address());
        studentUser.setAddress(studentAddress);
        Institution institution = institutionRepository.save(Institution.builder()
                .name("Escola do Aluno")
                .institutionType(InstitutionType.SCHOOL)
                .address(address())
                .build());
        studentService.selectInstitution(student.getId(), institution.getId());
        scheduleService.setRoundTripForDay(
                student.getId(), DayOfWeek.MONDAY, LocalTime.of(7, 0), LocalTime.of(18, 0));

        assertThat(studentService.getProfileStatus(student.getId()).complete()).isTrue();
        assertThat(linkService.findStudentsEligibleForRoute(driver.getId()))
                .extracting(Student::getId)
                .containsExactly(student.getId());

        linkService.endByStudent(student.getId(), link.getId());
        DriverStudentLink renewedLink = linkService.acceptInvite(
                student.getId(), replacementInvite.getToken());
        assertThat(renewedLink.getId()).isNotEqualTo(link.getId());
        assertThat(linkRepository.findAllByStudentId(student.getId())).hasSize(2);
        assertThat(link.getStatus()).isEqualTo(DriverStudentLinkStatus.ENDED);
        assertThat(link.getEndDate()).isNotNull();

        driverService.suspend(driver.getId(), "Suspensão administrativa");

        assertThat(inviteRepository.findById(replacementInvite.getId()).orElseThrow().getStatus())
                .isEqualTo(InviteStatus.REVOKED);
        assertThat(inviteRepository.findById(replacementInvite.getId()).orElseThrow().getRevokedAt())
                .isNotNull();
        assertThat(linkRepository.findById(renewedLink.getId()).orElseThrow().getStatus())
                .isEqualTo(DriverStudentLinkStatus.ACTIVE);
        assertThatThrownBy(() -> linkService.findStudentsEligibleForRoute(driver.getId()))
                .isInstanceOf(BusinessRuleException.class);
    }

    private User user(String name, String email, Role role, Address address) {
        return User.builder()
                .name(name)
                .email(email)
                .password("encoded-password")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .role(role)
                .address(address)
                .build();
    }

    private Address address() {
        return Address.builder()
                .street("Rua Principal")
                .number("10")
                .neighborhood("Centro")
                .city("Salvador")
                .state("BA")
                .zipCode("40000-000")
                .build();
    }
}
