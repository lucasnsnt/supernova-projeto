package ink.lucasnsnt.supernovaprojeto.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recurring_route_enrollments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecurringRouteEnrollment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "route_id", nullable = false) private RecurringRoute route;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id", nullable = false) private Student student;
    @Column(nullable = false) private boolean outboundEnabled;
    @Column(nullable = false) private boolean returnEnabled;
    @Column(nullable = false) private boolean active;
    @Column(nullable = false) private LocalDateTime requestedAt;
}
