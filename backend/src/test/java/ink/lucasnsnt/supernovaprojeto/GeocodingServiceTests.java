package ink.lucasnsnt.supernovaprojeto;

import ink.lucasnsnt.supernovaprojeto.config.DailyTransportProperties;
import ink.lucasnsnt.supernovaprojeto.exceptions.BusinessRuleException;
import ink.lucasnsnt.supernovaprojeto.models.Address;
import ink.lucasnsnt.supernovaprojeto.dtos.common.AddressRequest;
import ink.lucasnsnt.supernovaprojeto.services.GeocodingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;

class GeocodingServiceTests {
    private GeocodingService service;
    private HttpServer server;
    private final ArrayDeque<String> responses = new ArrayDeque<>();
    private final List<String> requests = new ArrayList<>();
    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/pelias/v1/search", exchange -> {
            requests.add(exchange.getRequestURI().getRawQuery());
            requests.add(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = responses.removeFirst().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        var properties = new DailyTransportProperties();
        properties.getGeocoding().setEnabled(true);
        properties.getGeocoding().setApiKey("test-key");
        properties.getGeocoding().setEndpoint("http://127.0.0.1:" + server.getAddress().getPort());
        service = new GeocodingService(RestClient.builder(), properties);
    }
    @AfterEach
    void stopServer() { server.stop(0); }
    @Test
    void resolvesFullAddressAndReusesCoordinates() {
        responses.add(response(0.95, "BRA"));
        Address address = address();
        service.resolve(address);
        assertThat(address.getLatitude()).isEqualByComparingTo("-12.97");
        assertThat(address.getLongitude()).isEqualByComparingTo("-38.50");
        service.resolve(address);
        assertThat(requests).hasSize(2);
        assertThat(java.net.URLDecoder.decode(requests.getFirst(), StandardCharsets.UTF_8))
                .contains("text=Rua A & B 10, Centro, Salvador, BA, 40000-000")
                .contains("boundary.country=BR");
        assertThat(requests.get(1)).isEqualTo("test-key");
        assertThat(responses).isEmpty();
    }
    @Test
    void rejectsLowConfidenceAndForeignMatches() {
        responses.add(response(0.4, "BRA"));
        responses.add(response(0.9, "USA"));
        assertThatThrownBy(() -> service.resolve(address())).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.resolve(address())).isInstanceOf(BusinessRuleException.class);
        assertThat(responses).isEmpty();
    }
    @Test
    void exposesActionableErrorForUnknownAddress() {
        responses.add("{\"features\":[]}");
        Address address = address();
        assertThatThrownBy(() -> service.resolve(address())).hasMessageContaining("Confira rua");
        assertThat(address.getLatitude()).isNull();
    }
    @Test
    void doesNotExposeProviderCredentialsOrErrors() {
        responses.add("{\"error\":\"test-key\"}");
        assertThatThrownBy(() -> service.resolve(address())).hasMessageContaining("Tente novamente")
                .hasMessageNotContaining("test-key");
    }
    @Test
    void disabledGeocodingDoesNotInventCoordinates() {
        var disabled = new GeocodingService(RestClient.builder(), new DailyTransportProperties());
        Address address = address();
        disabled.resolve(address);
        assertThat(address.getLatitude()).isNull();
    }
    @Test
    void previewsCoordinatesWithoutPersistingAnAddress() {
        responses.add(response(0.95, "BRA"));
        var preview = service.preview(new AddressRequest("Rua A & B", "10", null,
                "Centro", "Salvador", "BA", "40000-000"));
        assertThat(preview.latitude()).isEqualByComparingTo("-12.97");
        assertThat(preview.longitude()).isEqualByComparingTo("-38.50");
    }
    private String response(double confidence, String country) {
        return "{\"features\":[{\"geometry\":{\"type\":\"Point\",\"coordinates\":[-38.50,-12.97]},"
                + "\"properties\":{\"confidence\":" + confidence + ",\"country_a\":\"" + country + "\"}}]}";
    }
    private Address address() {
        return Address.builder().street("Rua A & B").number("10").neighborhood("Centro")
                .city("Salvador").state("BA").zipCode("40000-000").build();
    }
}
