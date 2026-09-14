package ink.lucasnsnt.supernovaprojeto.services.mail;

import ink.lucasnsnt.supernovaprojeto.events.EmailVerificationRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EmailVerificationEventListener {

    private final EmailVerificationSender sender;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVerificationRequested(EmailVerificationRequestedEvent event) {
        sender.sendVerificationCode(event.email(), event.code());
    }
}
