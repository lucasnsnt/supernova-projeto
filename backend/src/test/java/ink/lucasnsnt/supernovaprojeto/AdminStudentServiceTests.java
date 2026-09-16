package ink.lucasnsnt.supernovaprojeto;

import ink.lucasnsnt.supernovaprojeto.models.Address;
import ink.lucasnsnt.supernovaprojeto.models.Institution;
import ink.lucasnsnt.supernovaprojeto.models.Student;
import ink.lucasnsnt.supernovaprojeto.models.User;
import ink.lucasnsnt.supernovaprojeto.repositories.StudentRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.StudentScheduleRepository;
import ink.lucasnsnt.supernovaprojeto.services.AdminStudentService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminStudentServiceTests {

    private final StudentRepository studentRepository = mock(StudentRepository.class);
    private final StudentScheduleRepository scheduleRepository = mock(StudentScheduleRepository.class);
    private final AdminStudentService service = new AdminStudentService(studentRepository, scheduleRepository);

    @Test
    void shouldExposeStudentReadinessToTheAdministrator() {
        Address address = Address.builder().street("Rua A").number("10").neighborhood("Centro")
                .city("Aracaju").state("SE").zipCode("49000-000").build();
        User user = User.builder().id(4L).name("Aluno").email("aluno@example.com").phone("79999999999")
                .registrationDate(LocalDateTime.of(2026, 9, 16, 9, 0)).address(address).build();
        Institution institution = Institution.builder().id(8L).name("Universidade").address(address).build();
        Student student = Student.builder().id(4L).user(user).institution(institution)
                .driverLinks(new ArrayList<>()).build();
        when(studentRepository.findAll()).thenReturn(List.of(student));
        when(scheduleRepository.countByStudentId(4L)).thenReturn(2L);

        var result = service.findAll().getFirst();

        assertThat(result.institutionName()).isEqualTo("Universidade");
        assertThat(result.scheduleCount()).isEqualTo(2);
        assertThat(result.profileComplete()).isTrue();
        assertThat(result.linkStatus()).isNull();
    }
}
