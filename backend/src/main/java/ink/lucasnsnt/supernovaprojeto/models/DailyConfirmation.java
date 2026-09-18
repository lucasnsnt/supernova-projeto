package ink.lucasnsnt.supernovaprojeto.models;

import ink.lucasnsnt.supernovaprojeto.models.enums.DailyConfirmationStatus;
import ink.lucasnsnt.supernovaprojeto.models.enums.Direction;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "daily_confirmations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_daily_confirmation",
                columnNames = {"student_id", "service_date", "direction", "recurring_route_id", "scheduled_time"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyConfirmation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_route_id")
    private RecurringRoute recurringRoute;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private LocalDate serviceDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Direction direction;

    @Column(nullable = false)
    private LocalTime scheduledTime;

    private LocalTime academicTime;

    @Column(nullable = false)
    private LocalDateTime preliminaryDepartureAt;

    @Column(nullable = false)
    private LocalDateTime availableAt;

    @Column(nullable = false)
    private LocalDateTime responseDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DailyConfirmationStatus status = DailyConfirmationStatus.PENDING;

    private LocalDateTime respondedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
