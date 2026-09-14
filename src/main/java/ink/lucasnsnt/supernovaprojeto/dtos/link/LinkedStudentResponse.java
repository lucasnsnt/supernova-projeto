package ink.lucasnsnt.supernovaprojeto.dtos.link;

import ink.lucasnsnt.supernovaprojeto.dtos.common.AddressResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.student.ScheduleResponse;
import ink.lucasnsnt.supernovaprojeto.models.DriverStudentLink;
import ink.lucasnsnt.supernovaprojeto.models.enums.DriverStudentLinkStatus;

import java.time.LocalDate;
import java.util.List;

public record LinkedStudentResponse(
        Long linkId,
        Long studentId,
        String name,
        String phone,
        AddressResponse address,
        Long institutionId,
        String institutionName,
        boolean profileComplete,
        List<ScheduleResponse> schedules,
        DriverStudentLinkStatus status,
        LocalDate startDate,
        LocalDate endDate) {

    public static LinkedStudentResponse from(DriverStudentLink link) {
        var student = link.getStudent();
        var institution = student.getInstitution();
        var schedules = student.getSchedules().stream()
                .map(schedule -> new ScheduleResponse(schedule.getId(), schedule.getDayOfWeek(),
                        schedule.getTime(), schedule.getDirection()))
                .toList();
        return new LinkedStudentResponse(link.getId(), student.getId(), student.getUser().getName(),
                student.getUser().getPhone(), AddressResponse.from(student.getUser().getAddress()),
                institution == null ? null : institution.getId(), institution == null ? null : institution.getName(),
                student.isProfileComplete(), schedules, link.getStatus(), link.getStartDate(), link.getEndDate());
    }
}
