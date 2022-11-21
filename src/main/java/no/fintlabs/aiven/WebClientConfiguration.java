package no.fintlabs.aiven;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfiguration {

    private final AivenProperties aivenProperties;

    public WebClientConfiguration(AivenProperties aivenProperties) {
        this.aivenProperties = aivenProperties;
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(aivenProperties.getBaseUrl())
                .filter(authHeader())
                .build();
    }

    private ExchangeFilterFunction authHeader() {
        return (request, next) -> next.exchange(
                ClientRequest
                        .from(request)
                        .headers((headers) -> headers.setBearerAuth(aivenProperties.getToken()))
                        .build()
        );
    }
}
