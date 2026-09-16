package ink.lucasnsnt.supernovaprojeto.repositories;

import ink.lucasnsnt.supernovaprojeto.models.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    @Override
    @EntityGraph(attributePaths = {
            "user", "user.address", "institution", "institution.address",
            "driverLinks", "driverLinks.driver", "driverLinks.driver.user"
    })
    List<Student> findAll();

    List<Student> findAllByInstitutionId(Long institutionId);

    boolean existsByInstitutionId(Long institutionId);
}
