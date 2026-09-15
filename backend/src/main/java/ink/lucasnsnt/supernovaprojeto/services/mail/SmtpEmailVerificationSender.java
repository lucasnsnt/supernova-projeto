package ink.lucasnsnt.supernovaprojeto.services.mail;

import ink.lucasnsnt.supernovaprojeto.config.SecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mail.delivery", havingValue = "smtp")
public class SmtpEmailVerificationSender implements EmailVerificationSender {

    private final JavaMailSender mailSender;
    private final SecurityProperties securityProperties;

    @Value("${spring.mail.username}")
    private String sender;

    @Override
    public void sendVerificationCode(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(email);
        message.setSubject("Código de verificação Supernova");
        message.setText("Seu código de verificação é: " + code
                + "\nEle expira em " + securityProperties.getVerificationCodeTtl().toMinutes() + " minutos.");
        mailSender.send(message);
    }
}
