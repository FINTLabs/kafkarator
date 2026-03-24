package no.fintlabs.aiven;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "fint.aiven")
public class AivenProperties {

    private String baseUrl;
    private String token;
    private String project = "fintlabs";
    private String service;
    private String kafkaBootstrapServers;
    private Duration certificateRotationThreshold = Duration.ofDays(30);

}
