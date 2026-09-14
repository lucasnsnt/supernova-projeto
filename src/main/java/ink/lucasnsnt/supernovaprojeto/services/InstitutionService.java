package ink.lucasnsnt.supernovaprojeto.services;

import ink.lucasnsnt.supernovaprojeto.dtos.common.AddressRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.institution.InstitutionRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.institution.InstitutionResponse;
import ink.lucasnsnt.supernovaprojeto.exceptions.ResourceConflictException;
import ink.lucasnsnt.supernovaprojeto.exceptions.ResourceNotFoundException;
import ink.lucasnsnt.supernovaprojeto.models.Address;
import ink.lucasnsnt.supernovaprojeto.models.Institution;
import ink.lucasnsnt.supernovaprojeto.models.enums.InstitutionType;
import ink.lucasnsnt.supernovaprojeto.repositories.InstitutionRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.StudentRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
public class InstitutionService {

    private final InstitutionRepository institutionRepository;
    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public List<InstitutionResponse> findAll(String name, InstitutionType type) {
        List<Institution> institutions;
        if (name != null && !name.isBlank() && type != null) {
            institutions = institutionRepository
                    .findAllByNameContainingIgnoreCaseAndInstitutionType(name.trim(), type);
        } else if (name != null && !name.isBlank()) {
            institutions = institutionRepository.findAllByNameContainingIgnoreCase(name.trim());
        } else if (type != null) {
            institutions = institutionRepository.findAllByInstitutionType(type);
        } else {
            institutions = institutionRepository.findAll();
        }
        return institutions.stream().map(InstitutionResponse::from).toList();
    }

    @Transactional
    public InstitutionResponse create(@Valid InstitutionRequest request) {
        Institution institution = Institution.builder()
                .name(request.name().trim())
                .institutionType(request.type())
                .address(address(request.address()))
                .build();
        return InstitutionResponse.from(institutionRepository.save(institution));
    }

    @Transactional
    public InstitutionResponse update(@NotNull Long id, @Valid InstitutionRequest request) {
        Institution institution = find(id);
        institution.setName(request.name().trim());
        institution.setInstitutionType(request.type());
        copyAddress(institution.getAddress(), request.address());
        return InstitutionResponse.from(institution);
    }

    @Transactional
    public void delete(@NotNull Long id) {
        Institution institution = find(id);
        if (studentRepository.existsByInstitutionId(id)) {
            throw new ResourceConflictException("A instituição possui alunos vinculados e não pode ser excluída");
        }
        institutionRepository.delete(institution);
    }

    private Institution find(Long id) {
        return institutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Instituição", id));
    }

    private Address address(AddressRequest request) {
        Address address = new Address();
        copyAddress(address, request);
        return address;
    }

    private void copyAddress(Address target, AddressRequest source) {
        target.setStreet(source.street().trim());
        target.setNumber(source.number().trim());
        target.setComplement(source.complement());
        target.setNeighborhood(source.neighborhood().trim());
        target.setCity(source.city().trim());
        target.setState(source.state().trim().toUpperCase());
        target.setZipCode(source.zipCode().trim());
    }
}
