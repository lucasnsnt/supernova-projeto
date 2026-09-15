package ink.lucasnsnt.supernovaprojeto.models;

import ink.lucasnsnt.supernovaprojeto.models.enums.Direction;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;


@Entity
@Table(
        name = "student_schedules",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_student_schedule",
                columnNames = {"student_id", "day_of_week", "time", "direction"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(nullable = false)
    private LocalTime time;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Direction direction;
}
