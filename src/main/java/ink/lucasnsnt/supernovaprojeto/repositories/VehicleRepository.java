package ink.lucasnsnt.supernovaprojeto.repositories;

import ink.lucasnsnt.supernovaprojeto.models.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByLicensePlateIgnoreCase(String licensePlate);

    boolean existsByLicensePlateIgnoreCase(String licensePlate);

    List<Vehicle> findAllByDriverId(Long driverId);
}
