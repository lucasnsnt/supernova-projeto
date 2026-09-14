package ink.lucasnsnt.supernovaprojeto.controllers;

import ink.lucasnsnt.supernovaprojeto.dtos.institution.InstitutionRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.institution.InstitutionResponse;
import ink.lucasnsnt.supernovaprojeto.models.enums.InstitutionType;
import ink.lucasnsnt.supernovaprojeto.services.InstitutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/institutions")
@RequiredArgsConstructor
public class InstitutionController {

    private final InstitutionService institutionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    public List<InstitutionResponse> findAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) InstitutionType type) {
        return institutionService.findAll(name, type);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public InstitutionResponse create(@Valid @RequestBody InstitutionRequest request) {
        return institutionService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public InstitutionResponse update(
            @PathVariable Long id,
            @Valid @RequestBody InstitutionRequest request) {
        return institutionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        institutionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
