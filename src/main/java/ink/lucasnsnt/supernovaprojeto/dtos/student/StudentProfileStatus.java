package ink.lucasnsnt.supernovaprojeto.dtos.student;

public record StudentProfileStatus(
        boolean hasAddress,
        boolean hasInstitution,
        boolean hasOutboundSchedule,
        boolean hasReturnSchedule,
        boolean complete) {
}
