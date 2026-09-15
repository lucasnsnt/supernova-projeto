package ink.lucasnsnt.supernovaprojeto.repositories;

import ink.lucasnsnt.supernovaprojeto.models.Driver;
import ink.lucasnsnt.supernovaprojeto.models.enums.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {

    Optional<Driver> findByCnh(String cnh);

    boolean existsByCnh(String cnh);

    boolean existsByCnhAndIdNot(String cnh, Long id);

    @EntityGraph(attributePaths = {"user", "user.address"})
    List<Driver> findAllByStatus(DriverStatus status);

    @Override
    @EntityGraph(attributePaths = {"user", "user.address"})
    List<Driver> findAll();
}
