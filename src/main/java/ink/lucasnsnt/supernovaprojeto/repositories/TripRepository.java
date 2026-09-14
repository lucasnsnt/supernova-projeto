package ink.lucasnsnt.supernovaprojeto.repositories;

import ink.lucasnsnt.supernovaprojeto.models.Trip;
import ink.lucasnsnt.supernovaprojeto.models.enums.TripStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    boolean existsByVehicleId(Long vehicleId);

    @EntityGraph(attributePaths = {
            "vehicle", "participants", "participants.student", "participants.student.user",
            "participants.student.institution", "participants.confirmation"
    })
    List<Trip> findAllByDriverIdAndServiceDateOrderByPlannedDepartureAt(
            Long driverId, LocalDate serviceDate);

    @EntityGraph(attributePaths = {
            "vehicle", "driver", "driver.user", "participants", "participants.student",
            "participants.student.user", "participants.student.institution", "participants.confirmation"
    })
    Optional<Trip> findByIdAndDriverId(Long id, Long driverId);

    @EntityGraph(attributePaths = {
            "vehicle", "driver", "driver.user", "participants", "participants.student",
            "participants.student.user", "participants.student.institution", "participants.confirmation"
    })
    List<Trip> findDistinctByParticipantsStudentIdAndServiceDateOrderByPlannedDepartureAt(
            Long studentId, LocalDate serviceDate);

    List<Trip> findAllByStatusAndPlannedDepartureAtBetween(
            TripStatus status, LocalDateTime start, LocalDateTime end);
}
