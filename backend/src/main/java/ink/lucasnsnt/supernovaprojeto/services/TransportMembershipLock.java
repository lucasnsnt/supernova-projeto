package ink.lucasnsnt.supernovaprojeto.services;
import ink.lucasnsnt.supernovaprojeto.models.enums.DriverStudentLinkStatus;
import ink.lucasnsnt.supernovaprojeto.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class TransportMembershipLock {
    private final DriverStudentLinkRepository links;
    private final DriverRepository drivers;
    public void student(Long studentId) {
        links.findFirstByStudentIdAndStatus(studentId, DriverStudentLinkStatus.ACTIVE)
                .ifPresent(link -> drivers.lockById(link.getDriver().getId()));
    }
    public void driver(Long driverId) { drivers.lockById(driverId); }
}
