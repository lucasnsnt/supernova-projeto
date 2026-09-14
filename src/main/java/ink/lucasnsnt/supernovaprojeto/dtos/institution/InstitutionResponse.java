package ink.lucasnsnt.supernovaprojeto.dtos.institution;

import ink.lucasnsnt.supernovaprojeto.dtos.common.AddressResponse;
import ink.lucasnsnt.supernovaprojeto.models.Institution;
import ink.lucasnsnt.supernovaprojeto.models.enums.InstitutionType;

public record InstitutionResponse(
        Long id, String name, InstitutionType type, AddressResponse address) {

    public static InstitutionResponse from(Institution institution) {
        return new InstitutionResponse(institution.getId(), institution.getName(),
                institution.getInstitutionType(), AddressResponse.from(institution.getAddress()));
    }
}
