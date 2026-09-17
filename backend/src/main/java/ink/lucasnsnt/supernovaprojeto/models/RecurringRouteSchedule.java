package ink.lucasnsnt.supernovaprojeto.models;

import ink.lucasnsnt.supernovaprojeto.models.enums.Direction;
import jakarta.persistence.*;
import lombok.*;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "recurring_route_schedules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecurringRouteSchedule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "route_id", nullable = false) private RecurringRoute route;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private DayOfWeek dayOfWeek;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Direction direction;
    @Column(nullable = false) private LocalTime departureTime;
    @Column(nullable = false) private LocalTime responseDeadlineTime;
}
