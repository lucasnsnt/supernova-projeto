package ink.lucasnsnt.supernovaprojeto.services.mail;

public interface EmailVerificationSender {

    void sendVerificationCode(String email, String code);
}
