package no.fintlabs;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "fint.aiven")
public class AivenProperties {

    private String baseUrl;
    private String token;
    private String project = "fintlabs";
    private String service;

}
