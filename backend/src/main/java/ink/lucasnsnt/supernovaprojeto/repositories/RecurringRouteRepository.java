package ink.lucasnsnt.supernovaprojeto.repositories;

import ink.lucasnsnt.supernovaprojeto.models.RecurringRoute;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RecurringRouteRepository extends JpaRepository<RecurringRoute, Long> {
    @EntityGraph(attributePaths = {"vehicle", "schedules", "institutions", "institutions.institution"})
    List<RecurringRoute> findAllByDriverIdOrderByName(Long driverId);
    @EntityGraph(attributePaths = {"driver", "vehicle", "schedules", "institutions", "institutions.institution"})
    Optional<RecurringRoute> findByIdAndDriverId(Long id, Long driverId);
    @EntityGraph(attributePaths = {"driver", "vehicle", "schedules", "institutions", "institutions.institution"})
    List<RecurringRoute> findAllByActiveTrueOrderByName();
}
