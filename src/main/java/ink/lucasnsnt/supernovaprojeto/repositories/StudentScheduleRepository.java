package ink.lucasnsnt.supernovaprojeto.repositories;

import ink.lucasnsnt.supernovaprojeto.models.StudentSchedule;
import ink.lucasnsnt.supernovaprojeto.models.enums.Direction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface StudentScheduleRepository extends JpaRepository<StudentSchedule, Long> {

    List<StudentSchedule> findAllByStudentId(Long studentId);

    List<StudentSchedule> findAllByStudentIdAndDayOfWeek(Long studentId, DayOfWeek dayOfWeek);

    void deleteAllByStudentIdAndDayOfWeek(Long studentId, DayOfWeek dayOfWeek);

    List<StudentSchedule> findAllByStudentIdAndDirection(Long studentId, Direction direction);
}
