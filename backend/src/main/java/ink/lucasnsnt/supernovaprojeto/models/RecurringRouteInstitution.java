package ink.lucasnsnt.supernovaprojeto.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity
@Table(name = "recurring_route_institutions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecurringRouteInstitution {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "route_id", nullable = false) private RecurringRoute route;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "institution_id", nullable = false) private Institution institution;
    @Column(nullable = false) private Integer stopOrder;
    private LocalTime outboundArrivalBy;
    private LocalTime returnDepartureAt;
}
