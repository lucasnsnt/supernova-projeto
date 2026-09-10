package ink.lucasnsnt.supernovaprojeto.models;

import ink.lucasnsnt.supernovaprojeto.models.enums.DriverStudentLInkStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "driver_student_link")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverStudentLink {

    Driver driver;
    Student student;
    LocalDate startDate;
    LocalDate endDate;
    DriverStudentLInkStatus status;
}
