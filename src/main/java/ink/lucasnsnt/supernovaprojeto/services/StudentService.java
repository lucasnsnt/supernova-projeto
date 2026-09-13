package ink.lucasnsnt.supernovaprojeto.services;

import ink.lucasnsnt.supernovaprojeto.dtos.student.StudentProfileStatus;
import ink.lucasnsnt.supernovaprojeto.exceptions.BusinessRuleException;
import ink.lucasnsnt.supernovaprojeto.exceptions.ResourceConflictException;
import ink.lucasnsnt.supernovaprojeto.exceptions.ResourceNotFoundException;
import ink.lucasnsnt.supernovaprojeto.models.Institution;
import ink.lucasnsnt.supernovaprojeto.models.Student;
import ink.lucasnsnt.supernovaprojeto.models.User;
import ink.lucasnsnt.supernovaprojeto.models.enums.Direction;
import ink.lucasnsnt.supernovaprojeto.models.enums.Role;
import ink.lucasnsnt.supernovaprojeto.repositories.InstitutionRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.StudentRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.UserRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;

    @Transactional
    public Student register(@NotNull Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));
        if (user.getRole() != Role.STUDENT) {
            throw new BusinessRuleException("Somente um usuário com papel STUDENT pode virar aluno");
        }
        if (studentRepository.existsById(userId)) {
            throw new ResourceConflictException("O usuário já possui um cadastro de aluno");
        }

        Student student = Student.builder().user(user).build();
        user.setStudent(student);
        return studentRepository.save(student);
    }

    @Transactional(readOnly = true)
    public Student findById(@NotNull Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno", studentId));
    }

    @Transactional
    public Student selectInstitution(@NotNull Long studentId, @NotNull Long institutionId) {
        Student student = findById(studentId);
        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Instituição", institutionId));
        student.setInstitution(institution);
        return student;
    }

    @Transactional(readOnly = true)
    public StudentProfileStatus getProfileStatus(@NotNull Long studentId) {
        Student student = findById(studentId);
        boolean hasOutbound = student.getSchedules().stream()
                .anyMatch(schedule -> schedule.getDirection() == Direction.IDA);
        boolean hasReturn = student.getSchedules().stream()
                .anyMatch(schedule -> schedule.getDirection() == Direction.VOLTA);
        return new StudentProfileStatus(
                student.getUser().getAddress() != null,
                student.getInstitution() != null,
                hasOutbound,
                hasReturn,
                student.isProfileComplete());
    }
}
