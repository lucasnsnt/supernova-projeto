package ink.lucasnsnt.supernovaprojeto.dtos.student;

import ink.lucasnsnt.supernovaprojeto.dtos.account.AccountResponse;
import ink.lucasnsnt.supernovaprojeto.dtos.institution.InstitutionResponse;

public record StudentDetailsResponse(
        AccountResponse account,
        InstitutionResponse institution,
        StudentProfileStatus profileStatus) {
}
