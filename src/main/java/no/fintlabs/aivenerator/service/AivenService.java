package no.fintlabs.aivenerator.service;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.aivenerator.model.CreateUserRequest;
import no.fintlabs.aivenerator.model.CreateUserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class AivenService {

    @Value("${aiven.base_url}")
    private String baseUrl;
    @Value("${aiven.token}")
    private String token;

    public CreateUserResponse createUserForService(String project, String service_name, String username) {
        String uri = String.format("%s/project/%s/service/%s/user", baseUrl, project, service_name);
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername(username);

        HttpEntity<CreateUserRequest> entity = new HttpEntity<>(request, headers);
        CreateUserResponse response = restTemplate.postForObject(uri, entity, CreateUserResponse.class);
        return response;
    }
    // TODO: Method to create ACL from Topic, Username and permission

}
