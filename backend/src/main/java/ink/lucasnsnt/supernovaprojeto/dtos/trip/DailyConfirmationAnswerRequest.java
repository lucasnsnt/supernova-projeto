package ink.lucasnsnt.supernovaprojeto.dtos.trip;

import ink.lucasnsnt.supernovaprojeto.models.enums.DailyConfirmationStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record DailyConfirmationAnswerRequest(@NotNull DailyConfirmationStatus answer) {

    @AssertTrue(message = "a resposta deve ser YES ou NO")
    public boolean isAnswerValid() {
        return answer == DailyConfirmationStatus.YES || answer == DailyConfirmationStatus.NO;
    }
}
