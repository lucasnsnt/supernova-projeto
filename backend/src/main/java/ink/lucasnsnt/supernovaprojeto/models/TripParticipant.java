package ink.lucasnsnt.supernovaprojeto.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "trip_participants",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_trip_student", columnNames = {"trip_id", "student_id"}),
                @UniqueConstraint(name = "uk_trip_confirmation", columnNames = "confirmation_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "confirmation_id", nullable = false, unique = true)
    private DailyConfirmation confirmation;

    @Column(nullable = false)
    private Integer pickupOrder;

    @Column(nullable = false)
    private Integer dropoffOrder;

    private LocalDateTime estimatedPickupAt;

    private LocalDateTime estimatedDropoffAt;
}
