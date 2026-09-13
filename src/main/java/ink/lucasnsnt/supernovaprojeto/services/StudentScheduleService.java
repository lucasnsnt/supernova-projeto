package ink.lucasnsnt.supernovaprojeto.services;

import ink.lucasnsnt.supernovaprojeto.models.Student;
import ink.lucasnsnt.supernovaprojeto.models.StudentSchedule;
import ink.lucasnsnt.supernovaprojeto.models.enums.Direction;
import ink.lucasnsnt.supernovaprojeto.repositories.StudentScheduleRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
public class StudentScheduleService {

    private final StudentScheduleRepository scheduleRepository;
    private final StudentService studentService;

    @Transactional
    public List<StudentSchedule> setRoundTripForDay(
            @NotNull Long studentId,
            @NotNull DayOfWeek dayOfWeek,
            @NotNull LocalTime outboundTime,
            @NotNull LocalTime returnTime) {
        Student student = studentService.findById(studentId);

        student.getSchedules().removeIf(schedule -> schedule.getDayOfWeek() == dayOfWeek);
        scheduleRepository.flush();

        StudentSchedule outbound = StudentSchedule.builder()
                .student(student)
                .dayOfWeek(dayOfWeek)
                .time(outboundTime)
                .direction(Direction.IDA)
                .build();
        StudentSchedule returning = StudentSchedule.builder()
                .student(student)
                .dayOfWeek(dayOfWeek)
                .time(returnTime)
                .direction(Direction.VOLTA)
                .build();
        student.addSchedule(outbound);
        student.addSchedule(returning);
        return scheduleRepository.saveAll(List.of(outbound, returning));
    }

    @Transactional
    public void removeDay(@NotNull Long studentId, @NotNull DayOfWeek dayOfWeek) {
        Student student = studentService.findById(studentId);
        student.getSchedules().removeIf(schedule -> schedule.getDayOfWeek() == dayOfWeek);
    }

    @Transactional(readOnly = true)
    public List<StudentSchedule> findAllByStudent(@NotNull Long studentId) {
        studentService.findById(studentId);
        return scheduleRepository.findAllByStudentId(studentId);
    }
}
