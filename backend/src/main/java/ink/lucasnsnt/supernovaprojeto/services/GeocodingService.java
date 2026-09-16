package ink.lucasnsnt.supernovaprojeto.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ink.lucasnsnt.supernovaprojeto.config.DailyTransportProperties;
import ink.lucasnsnt.supernovaprojeto.exceptions.BusinessRuleException;
import ink.lucasnsnt.supernovaprojeto.models.Address;
import ink.lucasnsnt.supernovaprojeto.dtos.common.AddressRequest;
import ink.lucasnsnt.supernovaprojeto.dtos.common.GeocodePreviewResponse;
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
            throw new IllegalStateException("ORS_API_KEY é obrigatório para geocodificação");
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
            response = client.get().uri(uri -> uri.path("/pelias/v1/search")
                    .queryParam("text", "{address}").queryParam("boundary.country", "BR")
                    .queryParam("lang", "pt").queryParam("size", 1).build(query))
                    .header("Authorization", properties.getGeocoding().getApiKey())
                    .retrieve().body(GeocodeResponse.class);
        } catch (RestClientException exception) {
            // Do not expose the upstream URL, which includes the API key.
            throw new BusinessRuleException("Não foi possível localizar o endereço. Tente novamente em instantes");
        }
        if (response == null || response.error() != null) {
            throw new BusinessRuleException("Serviço de localização indisponível. Tente novamente em instantes");
        }
        if (response.features() == null || response.features().isEmpty()) {
            throw invalidAddress();
        }
        GeocodeFeature result = response.features().getFirst();
        List<BigDecimal> coordinates = result.geometry() == null ? null : result.geometry().coordinates();
        BigDecimal longitude = coordinates == null || coordinates.size() < 2 ? null : coordinates.get(0);
        BigDecimal latitude = coordinates == null || coordinates.size() < 2 ? null : coordinates.get(1);
        Double confidence = result.properties() == null ? null : result.properties().confidence();
        String country = result.properties() == null ? null : result.properties().country_a();
        if (latitude == null || longitude == null || confidence == null || confidence < 0.6
                || (country != null && !"BRA".equals(country) && !"BR".equals(country))
                || latitude.abs().compareTo(BigDecimal.valueOf(90)) > 0
                || longitude.abs().compareTo(BigDecimal.valueOf(180)) > 0) {
            throw invalidAddress();
        }
        address.setLatitude(latitude);
        address.setLongitude(longitude);
    }

    public GeocodePreviewResponse preview(AddressRequest request) {
        Address address = Address.builder().street(request.street()).number(request.number())
                .complement(request.complement()).neighborhood(request.neighborhood())
                .city(request.city()).state(request.state()).zipCode(request.zipCode()).build();
        resolve(address);
        if (address.getLatitude() == null || address.getLongitude() == null) {
            throw new BusinessRuleException("A localização por mapa não está habilitada");
        }
        return new GeocodePreviewResponse(address.getLatitude(), address.getLongitude());
    }

    private BusinessRuleException invalidAddress() {
        return new BusinessRuleException("Não foi possível localizar o endereço com precisão. Confira rua, número, cidade e CEP");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeocodeResponse(List<GeocodeFeature> features, Object error) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeocodeFeature(Geometry geometry, Properties properties) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Geometry(String type, List<BigDecimal> coordinates) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Properties(Double confidence, String country_a, String label) {}
}
