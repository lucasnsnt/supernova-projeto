package ink.lucasnsnt.supernovaprojeto.services.routing;

public interface RoutePlanningGateway {

    RoutePlanningResult optimize(RoutePlanningRequest request);
}
