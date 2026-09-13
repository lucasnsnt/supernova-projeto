package ink.lucasnsnt.supernovaprojeto.models;

import ink.lucasnsnt.supernovaprojeto.models.enums.Direction;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    private Long id;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id")
    private Institution institution;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StudentSchedule> schedules = new ArrayList<>();

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DriverStudentLink> driverLinks = new ArrayList<>();

    public void addSchedule(StudentSchedule schedule) {
        schedules.add(schedule);
        schedule.setStudent(this);
    }

    public void addDriverLink(DriverStudentLink link) {
        driverLinks.add(link);
        link.setStudent(this);
    }

    @Transient
    public boolean isProfileComplete() {
        if (institution == null || user == null || user.getAddress() == null) {
            return false;
        }

        return !schedules.isEmpty() && schedules.stream()
                .map(StudentSchedule::getDayOfWeek)
                .distinct()
                .allMatch(this::hasRoundTripOn);
    }

    private boolean hasRoundTripOn(DayOfWeek dayOfWeek) {
        boolean hasOutbound = schedules.stream().anyMatch(schedule ->
                schedule.getDayOfWeek() == dayOfWeek
                        && schedule.getDirection() == Direction.IDA);
        boolean hasReturn = schedules.stream().anyMatch(schedule ->
                schedule.getDayOfWeek() == dayOfWeek
                        && schedule.getDirection() == Direction.VOLTA);
        return hasOutbound && hasReturn;
    }
}
