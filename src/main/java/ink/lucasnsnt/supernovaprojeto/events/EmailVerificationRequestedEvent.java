package ink.lucasnsnt.supernovaprojeto.events;

public record EmailVerificationRequestedEvent(String email, String code) {
}
