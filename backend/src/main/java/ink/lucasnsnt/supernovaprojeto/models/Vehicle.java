package ink.lucasnsnt.supernovaprojeto.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    @Column(name = "manufacturing_year")
    private Integer year;

    @Column(nullable = false, unique = true)
    private String licensePlate;

    @Column(nullable = false)
    private Integer passengerCapacity;

    private String color;

    @Column(nullable = false)
    @Builder.Default
    private boolean defaultVehicle = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;
}
