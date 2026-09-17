package ink.lucasnsnt.supernovaprojeto.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recurring_routes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecurringRoute {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayOfWeek ASC, direction ASC, departureTime ASC")
    @Builder.Default
    private List<RecurringRouteSchedule> schedules = new ArrayList<>();

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stopOrder ASC")
    @Builder.Default
    private List<RecurringRouteInstitution> institutions = new ArrayList<>();

    public void addSchedule(RecurringRouteSchedule schedule) { schedules.add(schedule); schedule.setRoute(this); }
    public void addInstitution(RecurringRouteInstitution institution) { institutions.add(institution); institution.setRoute(this); }
}
