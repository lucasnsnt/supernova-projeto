package ink.lucasnsnt.supernovaprojeto.models;

import ink.lucasnsnt.supernovaprojeto.models.enums.DriverStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "drivers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Driver {

    @Id
    private Long id;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, unique = true)
    private String cnh;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DriverStatus status = DriverStatus.PENDING;

    private String statusReason;

    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operational_address_id")
    private Address operationalAddress;

    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Vehicle> vehicles = new ArrayList<>();

    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DriverInvite> invites = new ArrayList<>();

    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DriverStudentLink> studentLinks = new ArrayList<>();

    public void addVehicle(Vehicle vehicle) {
        vehicles.add(vehicle);
        vehicle.setDriver(this);
    }

    public void addInvite(DriverInvite invite) {
        invites.add(invite);
        invite.setDriver(this);
    }

    public void addStudentLink(DriverStudentLink link) {
        studentLinks.add(link);
        link.setDriver(this);
    }
}
