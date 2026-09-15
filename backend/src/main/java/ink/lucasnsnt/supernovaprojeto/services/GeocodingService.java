package ink.lucasnsnt.supernovaprojeto.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ink.lucasnsnt.supernovaprojeto.config.DailyTransportProperties;
import ink.lucasnsnt.supernovaprojeto.exceptions.BusinessRuleException;
import ink.lucasnsnt.supernovaprojeto.models.Address;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

@Service
public class GeocodingService {
    private final DailyTransportProperties properties;
    private final RestClient client;

    public GeocodingService(RestClient.Builder builder, DailyTransportProperties properties) {
        this.properties = properties;
        if (properties.getGeocoding().isEnabled()
                && (properties.getGeocoding().getApiKey() == null
                || properties.getGeocoding().getApiKey().isBlank())) {
            throw new IllegalStateException("GOOGLE_GEOCODING_API_KEY é obrigatório para geocodificação");
        }
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        client = builder.baseUrl(properties.getGeocoding().getEndpoint())
                .requestFactory(factory).build();
    }

    /** Resolves only missing coordinates; callers own the persistence transaction. */
    public void resolve(Address address) {
        if (address == null || (address.getLatitude() != null && address.getLongitude() != null)
                || !properties.getGeocoding().isEnabled()) {
            return;
        }
        GeocodeResponse response;
        try {
            String query = String.join(", ", address.getStreet() + " " + address.getNumber(),
                    address.getNeighborhood(), address.getCity(), address.getState(), address.getZipCode());
            response = client.get().uri(uri -> uri.path("/maps/api/geocode/json")
                    .queryParam("address", "{address}").queryParam("components", "country:BR")
                    .queryParam("language", "pt-BR").queryParam("key", "{key}")
                    .build(query, properties.getGeocoding().getApiKey()))
                    .retrieve().body(GeocodeResponse.class);
        } catch (RestClientException exception) {
            // Do not expose the upstream URL, which includes the API key.
            throw new BusinessRuleException("Não foi possível localizar o endereço. Tente novamente em instantes");
        }
        if (response == null || !"OK".equals(response.status())) {
            if (response != null && "ZERO_RESULTS".equals(response.status())) {
                throw invalidAddress();
            }
            throw new BusinessRuleException("Serviço de localização indisponível. Tente novamente em instantes");
        }
        if (response.results() == null || response.results().size() != 1) {
            throw invalidAddress();
        }
        GeocodeResult result = response.results().getFirst();
        Location location = result.geometry() == null ? null : result.geometry().location();
        if (Boolean.TRUE.equals(result.partial_match()) || location == null || location.lat() == null || location.lng() == null
                || location.lat().abs().compareTo(BigDecimal.valueOf(90)) > 0
                || location.lng().abs().compareTo(BigDecimal.valueOf(180)) > 0
                || !("ROOFTOP".equals(result.geometry().location_type())
                || "RANGE_INTERPOLATED".equals(result.geometry().location_type()))) {
            throw invalidAddress();
        }
        address.setLatitude(location.lat());
        address.setLongitude(location.lng());
    }

    private BusinessRuleException invalidAddress() {
        return new BusinessRuleException("Não foi possível localizar o endereço com precisão. Confira rua, número, cidade e CEP");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeocodeResponse(String status, List<GeocodeResult> results) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeocodeResult(Boolean partial_match, Geometry geometry) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Geometry(Location location, String location_type) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Location(BigDecimal lat, BigDecimal lng) {}
}
