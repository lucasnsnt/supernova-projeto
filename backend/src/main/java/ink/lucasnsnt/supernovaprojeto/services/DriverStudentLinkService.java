package ink.lucasnsnt.supernovaprojeto.services;

import ink.lucasnsnt.supernovaprojeto.exceptions.BusinessRuleException;
import ink.lucasnsnt.supernovaprojeto.exceptions.ResourceConflictException;
import ink.lucasnsnt.supernovaprojeto.exceptions.ResourceNotFoundException;
import ink.lucasnsnt.supernovaprojeto.models.Driver;
import ink.lucasnsnt.supernovaprojeto.models.DriverInvite;
import ink.lucasnsnt.supernovaprojeto.models.DriverStudentLink;
import ink.lucasnsnt.supernovaprojeto.models.Student;
import ink.lucasnsnt.supernovaprojeto.models.enums.DriverStudentLinkStatus;
import ink.lucasnsnt.supernovaprojeto.repositories.DriverStudentLinkRepository;
import ink.lucasnsnt.supernovaprojeto.dtos.link.LinkedStudentResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.link.StudentLinkResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
public class DriverStudentLinkService {

    private final DriverStudentLinkRepository linkRepository;
    private final DriverInviteService inviteService;
    private final DriverService driverService;
    private final StudentService studentService;
    private final Clock clock;
    private final TransportMembershipLock transportLock;

    @Transactional
    public DriverStudentLink acceptInvite(@NotNull Long studentId, @NotBlank String token) {
        Student student = studentService.findById(studentId);
        DriverInvite invite = inviteService.findUsableByToken(token);
        Driver driver = invite.getDriver();

        DriverStudentLink current = linkRepository
                .findFirstByStudentIdAndStatus(studentId, DriverStudentLinkStatus.ACTIVE)
                .orElse(null);
        if (current != null && current.getDriver().getId().equals(driver.getId())) {
            return current;
        }
        if (current != null) {
            throw new ResourceConflictException("O aluno já possui um motorista ativo");
        }

        DriverStudentLink link = DriverStudentLink.builder()
                .driver(driver)
                .student(student)
                .startDate(LocalDate.now(clock))
                .status(DriverStudentLinkStatus.ACTIVE)
                .build();
        driver.addStudentLink(link);
        student.addDriverLink(link);
        return linkRepository.save(link);
    }

    @Transactional
    public StudentLinkResponse acceptInviteResponse(
            @NotNull Long studentId, @NotBlank String token) {
        return StudentLinkResponse.from(acceptInvite(studentId, token));
    }

    @Transactional
    public DriverStudentLink endByStudent(@NotNull Long studentId, @NotNull Long linkId) {
        transportLock.student(studentId);
        DriverStudentLink link = findLink(linkId);
        if (!link.getStudent().getId().equals(studentId)) {
            throw new ResourceNotFoundException("Vínculo", linkId);
        }
        return end(link);
    }

    @Transactional
    public StudentLinkResponse endByStudentResponse(
            @NotNull Long studentId, @NotNull Long linkId) {
        return StudentLinkResponse.from(endByStudent(studentId, linkId));
    }

    @Transactional
    public DriverStudentLink endByDriver(@NotNull Long driverId, @NotNull Long linkId) {
        transportLock.driver(driverId);
        driverService.requireApproved(driverId);
        DriverStudentLink link = findLink(linkId);
        if (!link.getDriver().getId().equals(driverId)) {
            throw new ResourceNotFoundException("Vínculo", linkId);
        }
        return end(link);
    }

    @Transactional
    public LinkedStudentResponse endByDriverResponse(
            @NotNull Long driverId, @NotNull Long linkId) {
        return LinkedStudentResponse.from(endByDriver(driverId, linkId));
    }

    @Transactional(readOnly = true)
    public List<Student> findStudentsEligibleForRoute(@NotNull Long driverId) {
        driverService.requireApproved(driverId);
        return linkRepository.findAllByDriverIdAndStatus(driverId, DriverStudentLinkStatus.ACTIVE)
                .stream()
                .map(DriverStudentLink::getStudent)
                .filter(Student::isProfileComplete)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentLinkResponse> findHistoryByStudent(@NotNull Long studentId) {
        studentService.findById(studentId);
        return linkRepository.findAllByStudentId(studentId).stream()
                .map(StudentLinkResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LinkedStudentResponse> findHistoryByDriver(@NotNull Long driverId) {
        driverService.requireOperationalView(driverId);
        return linkRepository.findAllByDriverId(driverId).stream()
                .map(LinkedStudentResponse::from)
                .toList();
    }

    private DriverStudentLink findLink(Long linkId) {
        return linkRepository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("Vínculo", linkId));
    }

    private DriverStudentLink end(DriverStudentLink link) {
        if (link.getStatus() != DriverStudentLinkStatus.ACTIVE) {
            throw new BusinessRuleException("Somente um vínculo ativo pode ser encerrado");
        }
        link.setStatus(DriverStudentLinkStatus.ENDED);
        link.setEndDate(LocalDate.now(clock));
        return link;
    }
}
