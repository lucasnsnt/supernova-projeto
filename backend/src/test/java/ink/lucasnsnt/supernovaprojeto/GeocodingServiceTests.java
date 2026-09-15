package ink.lucasnsnt.supernovaprojeto;

import ink.lucasnsnt.supernovaprojeto.config.DailyTransportProperties;
import ink.lucasnsnt.supernovaprojeto.exceptions.BusinessRuleException;
import ink.lucasnsnt.supernovaprojeto.models.Address;
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
        server.createContext("/maps/api/geocode/json", exchange -> {
            requests.add(exchange.getRequestURI().getRawQuery());
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
        responses.add(response(false, "ROOFTOP"));
        Address address = address();
        service.resolve(address);
        assertThat(address.getLatitude()).isEqualByComparingTo("-12.97");
        assertThat(address.getLongitude()).isEqualByComparingTo("-38.50");
        service.resolve(address);
        assertThat(requests).hasSize(1);
        assertThat(java.net.URLDecoder.decode(requests.getFirst(), StandardCharsets.UTF_8))
                .contains("address=Rua A & B 10, Centro, Salvador, BA, 40000-000")
                .contains("components=country:BR");
        assertThat(responses).isEmpty();
    }
    @Test
    void rejectsPartialAndApproximateMatches() {
        responses.add(response(true, "ROOFTOP"));
        responses.add(response(false, "APPROXIMATE"));
        assertThatThrownBy(() -> service.resolve(address())).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.resolve(address())).isInstanceOf(BusinessRuleException.class);
        assertThat(responses).isEmpty();
    }
    @Test
    void exposesActionableErrorForUnknownAddress() {
        responses.add("{\"status\":\"ZERO_RESULTS\",\"results\":[]}");
        Address address = address();
        assertThatThrownBy(() -> service.resolve(address())).hasMessageContaining("Confira rua");
        assertThat(address.getLatitude()).isNull();
    }
    @Test
    void doesNotExposeProviderCredentialsOrErrors() {
        responses.add("{\"status\":\"REQUEST_DENIED\",\"error_message\":\"test-key\"}");
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
    private String response(boolean partial, String type) {
        return "{\"status\":\"OK\",\"results\":[{\"formatted_address\":\"Rua A, Brasil\",\"address_components\":[],\"partial_match\":" + partial
                + ",\"geometry\":{\"location_type\":\"" + type
                + "\",\"location\":{\"lat\":-12.97,\"lng\":-38.50}}}]}";
    }
    private Address address() {
        return Address.builder().street("Rua A & B").number("10").neighborhood("Centro")
                .city("Salvador").state("BA").zipCode("40000-000").build();
    }
}
