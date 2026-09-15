package ink.lucasnsnt.supernovaprojeto.services.routing;

import java.math.BigDecimal;

public record RoutePoint(BigDecimal latitude, BigDecimal longitude) {

    public boolean isComplete() {
        return latitude != null && longitude != null;
    }
}
