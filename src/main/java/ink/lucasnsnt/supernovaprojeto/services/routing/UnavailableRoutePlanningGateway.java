package ink.lucasnsnt.supernovaprojeto.services.routing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.transport.google.enabled",
        havingValue = "false",
        matchIfMissing = true)
public class UnavailableRoutePlanningGateway implements RoutePlanningGateway {

    @Override
    public RoutePlanningResult optimize(RoutePlanningRequest request) {
        return RoutePlanningResult.unavailable("O provedor de rotas não está configurado");
    }
}
