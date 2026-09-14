package ink.lucasnsnt.supernovaprojeto.repositories;

import ink.lucasnsnt.supernovaprojeto.models.Institution;
import ink.lucasnsnt.supernovaprojeto.models.enums.InstitutionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstitutionRepository extends JpaRepository<Institution, Long> {

    List<Institution> findAllByNameContainingIgnoreCase(String name);

    List<Institution> findAllByInstitutionType(InstitutionType institutionType);

    List<Institution> findAllByNameContainingIgnoreCaseAndInstitutionType(
            String name, InstitutionType institutionType);
}
