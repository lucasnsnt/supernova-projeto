package ink.lucasnsnt.supernovaprojeto;

import ink.lucasnsnt.supernovaprojeto.config.DailyTransportProperties;
import ink.lucasnsnt.supernovaprojeto.models.enums.Direction;
import ink.lucasnsnt.supernovaprojeto.services.routing.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleRoutePlanningGatewayTests {

    private MockRestServiceServer server;
    private GoogleRoutePlanningGateway gateway;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        DailyTransportProperties properties = new DailyTransportProperties();
        properties.getGoogle().setProjectId("test-project");
        gateway = new GoogleRoutePlanningGateway(builder, () -> "test-token", properties);
    }

    @Test
    void shouldSendOptimizationRequestAndMapRoute() {
        server.expect(once(), requestTo(
                        "https://routeoptimization.googleapis.com/v1/projects/test-project:optimizeTours"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.model.shipments[0].label").value("101"))
                .andExpect(jsonPath("$.model.shipments[0].pickups[0].arrivalLocation.latitude").value(-12.98))
                .andExpect(jsonPath("$.model.shipments[0].pickups[0].arrivalLocation.latitudeLongitude").doesNotExist())
                .andExpect(jsonPath("$.model.shipments[0].deliveries[0].timeWindows[0].softEndTime")
                        .value("2026-09-15T10:00:00Z"))
                .andExpect(jsonPath("$.model.shipments[0].deliveries[0].timeWindows[0].endTime")
                        .value("2026-09-15T10:45:00Z"))
                .andExpect(jsonPath("$.model.shipments[0].deliveries[0].timeWindows[0].costPerHourAfterSoftEndTime")
                        .value(100.0))
                .andExpect(jsonPath("$.model.vehicles[0].startLocation.longitude").value(-38.5014))
                .andExpect(jsonPath("$.model.vehicles[0].costPerHour").value(1.0))
                .andExpect(jsonPath("$.model.vehicles[0].loadLimits.passengers.maxLoad").value("4"))
                .andRespond(withSuccess("""
                        {
                          "routes": [{
                            "vehicleStartTime": "2026-09-15T08:45:00Z",
                            "routePolyline": {"points": "encoded-polyline"},
                            "visits": [
                              {"shipmentIndex": 0, "isPickup": true,
                               "startTime": "2026-09-15T09:00:00Z"},
                              {"shipmentIndex": 0, "isPickup": false,
                               "startTime": "2026-09-15T09:30:00Z"}
                            ]
                          }],
                          "skippedShipments": []
                        }
                        """, MediaType.APPLICATION_JSON));

        var result = gateway.optimize(request());

        assertThat(result.feasible()).isTrue();
        assertThat(result.provider()).isEqualTo("GOOGLE");
        assertThat(result.departureAt()).isEqualTo(LocalDateTime.of(2026, 9, 15, 5, 45));
        assertThat(result.encodedPolyline()).isEqualTo("encoded-polyline");
        assertThat(result.stops()).singleElement().satisfies(stop -> {
            assertThat(stop.confirmationId()).isEqualTo(101L);
            assertThat(stop.pickupOrder()).isOne();
            assertThat(stop.dropoffOrder()).isEqualTo(2);
            assertThat(stop.estimatedPickupAt()).isEqualTo(LocalDateTime.of(2026, 9, 15, 6, 0));
            assertThat(stop.estimatedDropoffAt()).isEqualTo(LocalDateTime.of(2026, 9, 15, 6, 30));
        });
        server.verify();
    }

    @Test
    void shouldMapProtoJsonOmittedZeroAndFalseFields() {
        server.expect(requestTo("https://routeoptimization.googleapis.com/v1/projects/test-project:optimizeTours"))
                .andRespond(withSuccess("""
                        {"metrics":{"totalCost":1},"routes":[{
                          "vehicleStartTime":"2026-09-15T08:45:00Z","vehicleLabel":"driver-10",
                          "visits":[{"isPickup":true,"startTime":"2026-09-15T09:00:00Z","shipmentLabel":"101"},
                                    {"startTime":"2026-09-15T09:30:00Z","visitRequestIndex":0}]}]}
                        """, MediaType.APPLICATION_JSON));
        var result = gateway.optimize(request());
        assertThat(result.feasible()).isTrue();
        assertThat(result.stops()).singleElement().satisfies(stop -> {
            assertThat(stop.confirmationId()).isEqualTo(101L);
            assertThat(stop.pickupOrder()).isOne();
            assertThat(stop.dropoffOrder()).isEqualTo(2);
        });
        server.verify();
    }

    @Test
    void shouldRejectRouteWithoutDepartureTime() {
        server.expect(requestTo(
                        "https://routeoptimization.googleapis.com/v1/projects/test-project:optimizeTours"))
                .andRespond(withSuccess("""
                        {"routes": [{"visits": [
                          {"shipmentIndex": 0, "isPickup": true,
                           "startTime": "2026-09-15T09:00:00Z"},
                          {"shipmentIndex": 0, "isPickup": false,
                           "startTime": "2026-09-15T09:30:00Z"}
                        ]}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(gateway.optimize(request()))
                .satisfies(result -> {
                    assertThat(result.feasible()).isFalse();
                    assertThat(result.issue()).contains("sem horário de saída");
                });
        server.verify();
    }

    @Test
    void shouldOmitNanosFromGoogleTimestamps() {
        server.expect(requestTo(
                        "https://routeoptimization.googleapis.com/v1/projects/test-project:optimizeTours"))
                .andExpect(jsonPath("$.model.vehicles[0].startTimeWindows[0].startTime")
                        .value("2026-09-15T08:45:12Z"))
                .andExpect(jsonPath("$.model.vehicles[0].startTimeWindows[0].endTime")
                        .value("2026-09-15T08:45:12Z"))
                .andRespond(withSuccess("""
                        {"routes": [{
                          "vehicleStartTime": "2026-09-15T08:45:12Z",
                          "visits": [
                            {"shipmentIndex": 0, "isPickup": true,
                             "startTime": "2026-09-15T09:00:00Z"},
                            {"shipmentIndex": 0, "isPickup": false,
                             "startTime": "2026-09-15T09:30:00Z"}
                          ]
                        }]}
                        """, MediaType.APPLICATION_JSON));

        RoutePlanningRequest request = request();
        request = new RoutePlanningRequest(request.driverId(), request.serviceDate(), request.direction(),
                request.vehicleCapacity(), request.start(), request.end(), request.passengers(),
                LocalDateTime.of(2026, 9, 15, 5, 45, 12, 987_654_321));

        assertThat(gateway.optimize(request).feasible()).isTrue();
        server.verify();
    }

    @Test
    void shouldIdentifySkippedConfirmationByStudentName() {
        server.expect(requestTo(
                        "https://routeoptimization.googleapis.com/v1/projects/test-project:optimizeTours"))
                .andRespond(withSuccess("""
                        {"routes": [], "skippedShipments": [{"index": 0, "label": "101"}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(gateway.optimize(request()).issue())
                .isEqualTo("A rota não conseguiu atender as confirmações: Aluno Teste");
        server.verify();
    }

    private RoutePlanningRequest request() {
        RoutePoint driver = point(-12.9714, -38.5014);
        RoutePassenger passenger = new RoutePassenger(
                101L,
                20L,
                "Aluno Teste",
                point(-12.9800, -38.5100),
                point(-12.9900, -38.5200),
                LocalDateTime.of(2026, 9, 15, 5, 30),
                LocalDateTime.of(2026, 9, 15, 6, 30),
                LocalDateTime.of(2026, 9, 15, 7, 45),
                LocalDateTime.of(2026, 9, 15, 7, 0));
        return new RoutePlanningRequest(10L, LocalDate.of(2026, 9, 15),
                Direction.IDA, 4, driver, driver, List.of(passenger));
    }

    private RoutePoint point(double latitude, double longitude) {
        return new RoutePoint(BigDecimal.valueOf(latitude), BigDecimal.valueOf(longitude));
    }
}
