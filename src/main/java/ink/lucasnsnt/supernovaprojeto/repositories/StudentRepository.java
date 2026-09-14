package ink.lucasnsnt.supernovaprojeto.repositories;

import ink.lucasnsnt.supernovaprojeto.models.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findAllByInstitutionId(Long institutionId);

    boolean existsByInstitutionId(Long institutionId);
}
