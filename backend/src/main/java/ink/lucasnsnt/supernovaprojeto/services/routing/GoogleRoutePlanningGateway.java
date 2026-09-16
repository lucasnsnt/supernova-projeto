package ink.lucasnsnt.supernovaprojeto.services.routing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ink.lucasnsnt.supernovaprojeto.config.DailyTransportProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.*;
import java.util.*;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.transport.google.enabled", havingValue = "true")
public class GoogleRoutePlanningGateway implements RoutePlanningGateway {

    private static final String PROVIDER = "GOOGLE";

    private final RestClient restClient;
    private final GoogleAccessTokenProvider tokenProvider;
    private final DailyTransportProperties properties;

    public GoogleRoutePlanningGateway(
            RestClient.Builder restClientBuilder,
            GoogleAccessTokenProvider tokenProvider,
            DailyTransportProperties properties) {
        if (properties.getGoogle().getProjectId() == null
                || properties.getGoogle().getProjectId().isBlank()) {
            throw new IllegalStateException(
                    "GOOGLE_CLOUD_PROJECT é obrigatório quando a otimização Google está habilitada");
        }
        this.restClient = restClientBuilder
                .baseUrl(properties.getGoogle().getEndpoint())
                .build();
        this.tokenProvider = tokenProvider;
        this.properties = properties;
    }

    @Override
    public RoutePlanningResult optimize(RoutePlanningRequest request) {
        try {
            GoogleOptimizeResponse response = restClient.post()
                    .uri("/v1/projects/{projectId}:optimizeTours", properties.getGoogle().getProjectId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(tokenProvider.accessToken()))
                    .body(requestBody(request))
                    .retrieve()
                    .body(GoogleOptimizeResponse.class);
            return result(request, response);
        } catch (RuntimeException exception) {
            log.error("Falha ao otimizar viagem do motorista {} para {}",
                    request.driverId(), request.serviceDate(), exception);
            return RoutePlanningResult.unavailable(
                    "Não foi possível calcular a rota no Google. Tente novamente em instantes");
        }
    }

    private Map<String, Object> requestBody(RoutePlanningRequest request) {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("globalStartTime", instant(request.serviceDate().atStartOfDay()));
        model.put("globalEndTime", instant(request.serviceDate().plusDays(1).atStartOfDay()));
        model.put("shipments", request.passengers().stream().map(this::shipment).toList());
        model.put("vehicles", List.of(vehicle(request)));
        return Map.of("model", model, "populatePolylines", true);
    }

    private Map<String, Object> shipment(RoutePassenger passenger) {
        Map<String, Object> shipment = new LinkedHashMap<>();
        shipment.put("label", passenger.confirmationId().toString());
        shipment.put("pickups", List.of(visit(
                passenger.pickup(), passenger.earliestPickupAt(), passenger.latestPickupAt())));
        shipment.put("deliveries", List.of(visit(
                passenger.dropoff(), null, passenger.latestDropoffAt())));
        shipment.put("loadDemands", Map.of("passengers", Map.of("amount", "1")));
        return shipment;
    }

    private Map<String, Object> vehicle(RoutePlanningRequest request) {
        Map<String, Object> vehicle = new LinkedHashMap<>();
        vehicle.put("label", "driver-" + request.driverId());
        // Relative optimization weights: minimize distance and total route duration.
        vehicle.put("costPerKilometer", 1.0);
        vehicle.put("costPerHour", 1.0);
        vehicle.put("loadLimits", Map.of(
                "passengers", Map.of("maxLoad", Integer.toString(request.vehicleCapacity()))));
        if (request.start() != null) {
            vehicle.put("startLocation", location(request.start()));
        }
        if (request.end() != null) {
            vehicle.put("endLocation", location(request.end()));
        }
        return vehicle;
    }

    private Map<String, Object> visit(
            RoutePoint point, LocalDateTime earliest, LocalDateTime latest) {
        Map<String, Object> visit = new LinkedHashMap<>();
        visit.put("arrivalLocation", location(point));
        if (earliest != null || latest != null) {
            Map<String, Object> window = new LinkedHashMap<>();
            if (earliest != null) {
                window.put("startTime", instant(earliest));
            }
            if (latest != null) {
                window.put("endTime", instant(latest));
            }
            visit.put("timeWindows", List.of(window));
        }
        return visit;
    }

    private Map<String, Object> location(RoutePoint point) {
        return Map.of("latitude", point.latitude(), "longitude", point.longitude());
    }

    private RoutePlanningResult result(
            RoutePlanningRequest request, GoogleOptimizeResponse response) {
        if (response == null || response.routes() == null || response.routes().isEmpty()) {
            return RoutePlanningResult.unavailable("O Google não retornou uma rota viável");
        }
        if (response.skippedShipments() != null && !response.skippedShipments().isEmpty()) {
            String skipped = response.skippedShipments().stream()
                    .map(GoogleSkippedShipment::label)
                    .filter(Objects::nonNull)
                    .collect(java.util.stream.Collectors.joining(", "));
            return RoutePlanningResult.unavailable(
                    "A rota não conseguiu atender as confirmações: " + skipped);
        }

        GoogleRoute route = response.routes().getFirst();
        LocalDateTime departureAt = localDateTime(route.vehicleStartTime());
        if (departureAt == null) {
            return RoutePlanningResult.unavailable("O Google retornou uma rota sem horário de saída");
        }
        Map<Integer, MutableStop> stops = new HashMap<>();
        List<GoogleVisit> visits = route.visits() == null ? List.of() : route.visits();
        for (int index = 0; index < visits.size(); index++) {
            GoogleVisit visit = visits.get(index);
            // ProtoJSON may omit scalar fields whose value is zero.
            int shipmentIndex = visit.shipmentIndex() == null ? 0 : visit.shipmentIndex();
            if (shipmentIndex < 0 || shipmentIndex >= request.passengers().size()) {
                continue;
            }
            MutableStop stop = stops.computeIfAbsent(shipmentIndex, ignored -> new MutableStop());
            LocalDateTime time = localDateTime(visit.startTime());
            if (Boolean.TRUE.equals(visit.isPickup())) {
                stop.pickupOrder = index + 1;
                stop.pickupAt = time;
            } else {
                stop.dropoffOrder = index + 1;
                stop.dropoffAt = time;
            }
        }
        if (stops.size() != request.passengers().size()) {
            return RoutePlanningResult.unavailable("O Google retornou uma rota incompleta");
        }

        List<RouteStopPlan> plans = new ArrayList<>();
        for (int index = 0; index < request.passengers().size(); index++) {
            RoutePassenger passenger = request.passengers().get(index);
            MutableStop stop = stops.get(index);
            if (stop.pickupOrder == 0 || stop.dropoffOrder == 0
                    || stop.pickupAt == null || stop.dropoffAt == null) {
                return RoutePlanningResult.unavailable("O Google retornou paradas incompletas");
            }
            plans.add(new RouteStopPlan(passenger.confirmationId(), stop.pickupOrder,
                    stop.dropoffOrder, stop.pickupAt, stop.dropoffAt));
        }
        String reference = "google-%d-%s-%s".formatted(
                request.driverId(), request.serviceDate(), UUID.randomUUID());
        return new RoutePlanningResult(true, PROVIDER, reference,
                departureAt,
                route.routePolyline() == null ? null : route.routePolyline().points(),
                null, plans);
    }

    private String instant(LocalDateTime value) {
        return value.atZone(properties.getZoneId()).toInstant().toString();
    }

    private LocalDateTime localDateTime(String value) {
        if (value == null) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.parse(value), properties.getZoneId());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleOptimizeResponse(
            List<GoogleRoute> routes,
            List<GoogleSkippedShipment> skippedShipments) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleRoute(
            String vehicleStartTime,
            GooglePolyline routePolyline,
            List<GoogleVisit> visits) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GooglePolyline(String points) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleVisit(Integer shipmentIndex, Boolean isPickup, String startTime) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleSkippedShipment(Integer index, String label) {
    }

    private static final class MutableStop {
        private int pickupOrder;
        private int dropoffOrder;
        private LocalDateTime pickupAt;
        private LocalDateTime dropoffAt;
    }
}
