package ink.lucasnsnt.supernovaprojeto.models;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

@Entity
@Table(name = "trip_locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripLocation implements Persistable<Long> {
    @Id
    @Column(name = "trip_id")
    private Long tripId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private Double accuracy;
    private Double heading;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    @Builder.Default
    private boolean newLocation = true;

    @Override
    public Long getId() {
        return tripId;
    }

    @Override
    public boolean isNew() {
        return newLocation;
    }

    @PostLoad
    @PostPersist
    void markPersisted() {
        newLocation = false;
    }
}
