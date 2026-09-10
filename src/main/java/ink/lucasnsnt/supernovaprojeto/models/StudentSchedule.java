package ink.lucasnsnt.supernovaprojeto.models;

import ink.lucasnsnt.supernovaprojeto.models.enums.Direction;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;


@Entity
@Table(name = "students_schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSchedule {

    Student student;

    DayOfWeek dayOfWeek;

    LocalTime time;

    Direction direction;

}
