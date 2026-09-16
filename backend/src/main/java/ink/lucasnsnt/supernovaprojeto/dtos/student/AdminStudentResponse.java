package ink.lucasnsnt.supernovaprojeto.dtos.student;

import ink.lucasnsnt.supernovaprojeto.dtos.common.AddressResponse;
import ink.lucasnsnt.supernovaprojeto.models.DriverStudentLink;
import ink.lucasnsnt.supernovaprojeto.models.Student;
import ink.lucasnsnt.supernovaprojeto.models.enums.DriverStudentLinkStatus;

import java.time.LocalDateTime;

public record AdminStudentResponse(
        Long id,
        String name,
        String email,
        String phone,
        LocalDateTime registrationDate,
        AddressResponse address,
        Long institutionId,
        String institutionName,
        long scheduleCount,
        Long driverId,
        String driverName,
        DriverStudentLinkStatus linkStatus,
        boolean profileComplete) {

    public static AdminStudentResponse from(Student student, long scheduleCount) {
        DriverStudentLink currentLink = student.getDriverLinks().stream()
                .filter(link -> link.getStatus() == DriverStudentLinkStatus.ACTIVE
                        || link.getStatus() == DriverStudentLinkStatus.PENDING)
                .findFirst().orElse(null);
        var institution = student.getInstitution();
        var user = student.getUser();
        return new AdminStudentResponse(student.getId(), user.getName(), user.getEmail(), user.getPhone(),
                user.getRegistrationDate(), AddressResponse.from(user.getAddress()),
                institution == null ? null : institution.getId(),
                institution == null ? null : institution.getName(), scheduleCount,
                currentLink == null ? null : currentLink.getDriver().getId(),
                currentLink == null ? null : currentLink.getDriver().getUser().getName(),
                currentLink == null ? null : currentLink.getStatus(),
                institution != null && user.getAddress() != null && scheduleCount > 0);
    }
}
