package ink.lucasnsnt.supernovaprojeto;

import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpServer;
import ink.lucasnsnt.supernovaprojeto.models.User;
import ink.lucasnsnt.supernovaprojeto.models.enums.Role;
import ink.lucasnsnt.supernovaprojeto.repositories.AddressRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.TripRepository;
import ink.lucasnsnt.supernovaprojeto.repositories.UserRepository;
import ink.lucasnsnt.supernovaprojeto.services.DailyTransportScheduler;
import ink.lucasnsnt.supernovaprojeto.services.mail.EmailVerificationSender;
import ink.lucasnsnt.supernovaprojeto.services.routing.GoogleAccessTokenProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Full application flow; only mail delivery and Google authentication are mocked.
 * Geocoding and route optimization use real HTTP against a local Google fixture. */
@SpringBootTest(properties = {"app.transport.scheduler-cron=-",
        "app.transport.google.enabled=true", "app.transport.google.project-id=test-project",
        "app.transport.geocoding.enabled=true", "app.transport.geocoding.api-key=test-key",
        "spring.datasource.url=jdbc:h2:mem:complete-transport-flow;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class CompleteTransportFlowTests {
    private static final AtomicInteger geocodes = new AtomicInteger();
    private static final AtomicInteger routes = new AtomicInteger();
    private static final HttpServer google = googleFixture();
    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private AddressRepository addresses;
    @Autowired private TripRepository trips;
    @Autowired private DailyTransportScheduler scheduler;
    @Autowired private org.springframework.security.oauth2.jwt.JwtDecoder decoder;
    @MockitoBean private EmailVerificationSender mail;
    @MockitoBean private GoogleAccessTokenProvider tokenProvider;
    @MockitoBean private Clock clock;
    @MockitoBean private ink.lucasnsnt.supernovaprojeto.services.PendingRegistrationCleanupService cleanup;
    private final ZoneId zone = ZoneId.of("America/Bahia");

    @DynamicPropertySource
    static void endpoints(DynamicPropertyRegistry registry) {
        String endpoint = "http://127.0.0.1:" + google.getAddress().getPort();
        registry.add("app.transport.geocoding.endpoint", () -> endpoint);
        registry.add("app.transport.google.endpoint", () -> endpoint);
    }
    @AfterAll static void stopGoogle() { google.stop(0); }

    @Test
    void shouldRegisterApproveConfirmPlanStartAndCompleteTrip() throws Exception {
        now("2026-09-15T10:00:00");
        when(tokenProvider.accessToken()).thenReturn("fixture-access-token");
        String driver = register("motorista@complete.test", "DRIVER", "11122233344");
        Long driverId = ((Number) read(get("/api/me"), driver, "$.id")).longValue();
        assertThat(read(get("/api/me"), driver, "$.driverStatus").toString()).isEqualTo("PENDING");
        String vehicle = """
                {"brand":"Fiat","model":"Ducato","licensePlate":"MVP1234","passengerCapacity":12}
                """;
        mvc.perform(post("/api/drivers/me/vehicles").header("Authorization", "Bearer " + driver)
                .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(vehicle))
                .andExpect(status().isUnprocessableContent());

        User admin = users.save(User.builder().name("Admin local").email("admin@complete.test")
                .password("test-only").dateOfBirth(LocalDate.of(1990, 1, 1)).phone("71999999999").role(Role.ADMIN).emailVerifiedAt(LocalDateTime.now(clock)).build());
        mvc.perform(post("/api/admin/drivers/{id}/approval", driverId)
                .with(jwt().jwt(jwt -> jwt.subject(admin.getId().toString())).authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")))
                .with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        assertThat(read(get("/api/me"), driver, "$.driverStatus").toString()).isEqualTo("APPROVED");
        String savedVehicle = send(post("/api/drivers/me/vehicles"), driver, vehicle);
        assertThat(JsonPath.<Boolean>read(savedVehicle, "$.defaultVehicle")).isTrue();
        String invite = JsonPath.read(send(post("/api/drivers/me/invites"), driver,
                "{\"validityDays\":7,\"replaceCurrent\":true}"), "$.token");

        String student = register("aluno@complete.test", "STUDENT", null);
        String institution = send(post("/api/institutions")
                .with(jwt().jwt(jwt -> jwt.subject(admin.getId().toString())).authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))),
                null, "{\"name\":\"Faculdade MVP\",\"type\":\"UNIVERSITY\",\"address\":" + address() + "}");
        Number institutionId = JsonPath.read(institution, "$.id");
        send(put("/api/students/me/institution"), student, "{\"institutionId\":" + institutionId + "}");
        send(put("/api/students/me/schedules/TUESDAY"), student, "{\"returnTime\":\"12:00\"}");
        send(post("/api/students/me/links"), student, "{\"token\":\"" + invite + "\"}");
        assertThat(read(get("/api/students/me/profile-status"), student, "$.complete").toString()).isEqualTo("true");
        assertThat(geocodes.get()).isEqualTo(3);
        assertThat(addresses.findAll()).allSatisfy(address -> {
            assertThat(address.getLatitude()).isNotNull();
            assertThat(address.getLongitude()).isNotNull();
        });

        // An old registration without coordinates must recover during planning.
        var oldAddress = addresses.findAll().getFirst();
        oldAddress.setLatitude(null); oldAddress.setLongitude(null); addresses.save(oldAddress);
        scheduler.processConfirmations();
        Number confirmationId = (Number) read(get("/api/students/me/daily-confirmations?date=2026-09-15"), student, "$[0].id");
        send(put("/api/students/me/daily-confirmations/{id}/answer", confirmationId), student, "{\"answer\":\"YES\"}");
        scheduler.processConfirmations();
        assertThat(trips.count()).isZero(); // Before the response deadline.
        now("2026-09-15T11:01:00");
        scheduler.processConfirmations();
        scheduler.processConfirmations(); // Must not duplicate the trip.
        assertThat(trips.count()).isOne();
        assertThat(geocodes.get()).isEqualTo(4);
        assertThat(routes.get()).isOne();

        // Obtain fresh access tokens after advancing the clock past token expiry.
        driver = login("motorista@complete.test"); student = login("aluno@complete.test");
        Number tripId = (Number) read(get("/api/drivers/me/trips?date=2026-09-15"), driver, "$[0].id");
        assertThat(read(get("/api/students/me/trips?date=2026-09-15"), student, "$[0].status").toString()).isEqualTo("PLANNED");
        assertThat(read(get("/api/drivers/me/trips?date=2026-09-15"), driver,
                "$[0].participants[0].pickupAddress.street").toString()).isEqualTo("Rua MVP");
        assertThat(read(get("/api/drivers/me/trips?date=2026-09-15"), driver,
                "$[0].participants[0].dropoffAddress.number").toString()).isEqualTo("10");
        send(post("/api/drivers/me/trips/{id}/start", tripId), driver, null);
        assertThat(read(get("/api/students/me/trips?date=2026-09-15"), student, "$[0].status").toString()).isEqualTo("IN_PROGRESS");
        send(post("/api/drivers/me/trips/{id}/completion", tripId), driver, null);
        assertThat(read(get("/api/students/me/trips?date=2026-09-15"), student, "$[0].status").toString()).isEqualTo("COMPLETED");
        assertThat(read(get("/api/students/me/trips?date=2026-09-15"), student, "$[0].completedAt")).isNotNull();
    }

    private void now(String time) {
        when(clock.getZone()).thenReturn(zone);
        when(clock.instant()).thenReturn(LocalDateTime.parse(time).atZone(zone).toInstant());
        var timestamps = new org.springframework.security.oauth2.jwt.JwtTimestampValidator();
        timestamps.setClock(clock);
        ((org.springframework.security.oauth2.jwt.NimbusJwtDecoder) decoder).setJwtValidator(
                new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                        timestamps, new org.springframework.security.oauth2.jwt.JwtIssuerValidator("supernova-api")));
    }
    private String register(String email, String role, String cnh) throws Exception {
        send(post("/api/auth/email-verification"), null, "{\"email\":\"" + email + "\"}");
        var code = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(mail).sendVerificationCode(eq(email), code.capture());
        String authorization = send(post("/api/auth/email-verification/confirm"), null,
                "{\"email\":\"" + email + "\",\"code\":\"" + code.getValue() + "\"}");
        String registration = "{\"registrationToken\":\"" + JsonPath.read(authorization, "$.registrationToken")
                + "\",\"name\":\"Pessoa MVP\",\"email\":\"" + email
                + "\",\"password\":\"Senha@123\",\"phone\":\"71999999999\",\"dateOfBirth\":\"2000-01-01\",\"role\":\""
                + role + "\",\"address\":" + address() + (cnh == null ? "" : ",\"cnh\":\"" + cnh + "\"") + "}";
        return JsonPath.read(send(post("/api/auth/register"), null, registration), "$.accessToken");
    }
    private String login(String email) throws Exception {
        return JsonPath.read(send(post("/api/auth/login"), null,
                "{\"email\":\"" + email + "\",\"password\":\"Senha@123\"}"), "$.accessToken");
    }
    private String send(MockHttpServletRequestBuilder request, String token, String body) throws Exception {
        if (token != null) request.header("Authorization", "Bearer " + token);
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(request.with(csrf())).andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
    }
    private Object read(MockHttpServletRequestBuilder request, String token, String path) throws Exception {
        return JsonPath.read(send(request, token, null), path);
    }
    private String address() {
        return """
                {"street":"Rua MVP","number":"10","neighborhood":"Centro","city":"Salvador","state":"BA","zipCode":"40000-000"}
                """;
    }
    private static HttpServer googleFixture() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/maps/api/geocode/json", exchange -> {
                geocodes.incrementAndGet();
                respond(exchange, """
                        {"status":"OK","results":[{"geometry":{"location_type":"ROOFTOP","location":{"lat":-12.97,"lng":-38.50}}}]}
                        """);
            });
            server.createContext("/v1/projects/test-project:optimizeTours", exchange -> {
                routes.incrementAndGet();
                exchange.getRequestBody().readAllBytes();
                respond(exchange, """
                        {"routes":[{"vehicleStartTime":"2026-09-15T14:45:00Z","visits":[
                        {"shipmentIndex":0,"isPickup":true,"startTime":"2026-09-15T15:00:00Z"},
                        {"shipmentIndex":0,"isPickup":false,"startTime":"2026-09-15T15:30:00Z"}]}]}
                        """);
            });
            server.start(); return server;
        } catch (java.io.IOException exception) { throw new IllegalStateException(exception); }
    }
    private static void respond(com.sun.net.httpserver.HttpExchange exchange, String json) throws java.io.IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (var output = exchange.getResponseBody()) { output.write(bytes); }
    }
}
