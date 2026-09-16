package ink.lucasnsnt.supernovaprojeto.controllers;

import ink.lucasnsnt.supernovaprojeto.dtos.student.AdminStudentResponse;
import ink.lucasnsnt.supernovaprojeto.services.AdminStudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/students")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminStudentController {

    private final AdminStudentService studentService;

    @GetMapping
    public List<AdminStudentResponse> findAll() {
        return studentService.findAll();
    }
}
