package ink.lucasnsnt.supernovaprojeto.dtos.link;

import ink.lucasnsnt.supernovaprojeto.models.DriverStudentLink;
import ink.lucasnsnt.supernovaprojeto.models.enums.DriverStudentLinkStatus;

import java.time.LocalDate;

public record StudentLinkResponse(
        Long id,
        Long driverId,
        String driverName,
        DriverStudentLinkStatus status,
        LocalDate startDate,
        LocalDate endDate) {

    public static StudentLinkResponse from(DriverStudentLink link) {
        return new StudentLinkResponse(link.getId(), link.getDriver().getId(),
                link.getDriver().getUser().getName(), link.getStatus(), link.getStartDate(), link.getEndDate());
    }
}
