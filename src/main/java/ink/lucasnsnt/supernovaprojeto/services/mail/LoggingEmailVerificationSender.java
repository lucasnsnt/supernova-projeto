package ink.lucasnsnt.supernovaprojeto.services.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.mail.delivery", havingValue = "log", matchIfMissing = true)
public class LoggingEmailVerificationSender implements EmailVerificationSender {

    @Override
    public void sendVerificationCode(String email, String code) {
        log.info("Código de verificação local para {}: {}", email, code);
    }
}
