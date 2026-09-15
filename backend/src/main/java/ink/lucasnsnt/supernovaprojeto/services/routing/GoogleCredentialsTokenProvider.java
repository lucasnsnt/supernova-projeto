package ink.lucasnsnt.supernovaprojeto.services.routing;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.transport.google.enabled", havingValue = "true")
public class GoogleCredentialsTokenProvider implements GoogleAccessTokenProvider {

    private static final String CLOUD_PLATFORM_SCOPE =
            "https://www.googleapis.com/auth/cloud-platform";

    private final GoogleCredentials credentials;

    public GoogleCredentialsTokenProvider() throws IOException {
        credentials = GoogleCredentials.getApplicationDefault()
                .createScoped(List.of(CLOUD_PLATFORM_SCOPE));
    }

    @Override
    public synchronized String accessToken() {
        try {
            credentials.refreshIfExpired();
            if (credentials.getAccessToken() == null) {
                credentials.refresh();
            }
            return credentials.getAccessToken().getTokenValue();
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível autenticar na API de rotas do Google", exception);
        }
    }
}
