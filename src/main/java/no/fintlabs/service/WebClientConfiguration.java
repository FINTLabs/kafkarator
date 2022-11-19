package no.fintlabs.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfiguration {

    @Value("${fint.aiven.base-url}")
    private String aivenBaseUrl;

    @Value("${fint.aiven.token}")
    private String token;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(aivenBaseUrl)
                .filter(authHeader())
                .build();
    }

    private ExchangeFilterFunction authHeader() {
        return (request, next) -> next.exchange(
                ClientRequest
                        .from(request)
                        .headers((headers) -> headers.setBearerAuth(token))
                        .build()
        );
    }
}
