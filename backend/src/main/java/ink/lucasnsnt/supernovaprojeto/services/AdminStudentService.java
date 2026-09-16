package ink.lucasnsnt.supernovaprojeto.services;

import ink.lucasnsnt.supernovaprojeto.dtos.student.AdminStudentResponse;
import ink.lucasnsnt.supernovaprojeto.repositories.StudentRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.StudentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminStudentService {

    private final StudentRepository studentRepository;
    private final StudentScheduleRepository scheduleRepository;

    @Transactional(readOnly = true)
    public List<AdminStudentResponse> findAll() {
        return studentRepository.findAll().stream()
                .map(student -> AdminStudentResponse.from(
                        student, scheduleRepository.countByStudentId(student.getId())))
                .toList();
    }
}
