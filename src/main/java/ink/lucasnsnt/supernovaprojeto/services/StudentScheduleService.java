package ink.lucasnsnt.supernovaprojeto.services;

import ink.lucasnsnt.supernovaprojeto.models.Student;
import ink.lucasnsnt.supernovaprojeto.models.StudentSchedule;
import ink.lucasnsnt.supernovaprojeto.models.enums.Direction;
import ink.lucasnsnt.supernovaprojeto.exceptions.BusinessRuleException;
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
        return setForDay(studentId, dayOfWeek, outboundTime, returnTime);
    }

    @Transactional
    public List<StudentSchedule> setForDay(
            @NotNull Long studentId,
            @NotNull DayOfWeek dayOfWeek,
            LocalTime outboundTime,
            LocalTime returnTime) {
        if (outboundTime == null && returnTime == null) {
            throw new BusinessRuleException("Informe ao menos um horário de ida ou volta");
        }
        if (outboundTime != null && returnTime != null && !returnTime.isAfter(outboundTime)) {
            throw new BusinessRuleException("O horário de volta precisa ser posterior ao horário de ida");
        }
        Student student = studentService.findById(studentId);

        student.getSchedules().removeIf(schedule -> schedule.getDayOfWeek() == dayOfWeek);
        scheduleRepository.flush();

        List<StudentSchedule> schedules = new java.util.ArrayList<>(2);
        if (outboundTime != null) {
            schedules.add(schedule(student, dayOfWeek, outboundTime, Direction.IDA));
        }
        if (returnTime != null) {
            schedules.add(schedule(student, dayOfWeek, returnTime, Direction.VOLTA));
        }
        schedules.forEach(student::addSchedule);
        return scheduleRepository.saveAll(schedules);
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

    private StudentSchedule schedule(
            Student student, DayOfWeek dayOfWeek, LocalTime time, Direction direction) {
        return StudentSchedule.builder()
                .student(student)
                .dayOfWeek(dayOfWeek)
                .time(time)
                .direction(direction)
                .build();
    }
}
