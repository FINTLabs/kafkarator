package no.fintlabs.aivenerator.service;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.aivenerator.model.CreateAclEntryRequest;
import no.fintlabs.aivenerator.model.CreateAclEntryResponse;
import no.fintlabs.aivenerator.model.CreateUserRequest;
import no.fintlabs.aivenerator.model.CreateUserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@Slf4j
@Component
public class AivenService {

    @Value("${aiven.base_url}")
    private String baseUrl;
    @Value("${aiven.token}")
    private String token;

    public CreateUserResponse createUserForService(String project, String service_name, String username) {
        log.debug("Creating user {} for service {}", username, service_name);
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

    public void deleteUserForService(String project, String service_name, String username) {
        log.debug("Deleting user {} from service {}", username, service_name);
        String uri = String.format("%s/project/%s/service/%s/user/%s", baseUrl, project, service_name, username);
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        restTemplate.exchange(uri, HttpMethod.DELETE, entity, String.class);
    }

    public CreateAclEntryResponse createACLEntryForTopic(String project, String service_name, String topic, String username, String permission) {
        log.debug("Creating ACL entry for topic {} for user {} with permission {}", topic, username, permission);
        String uri = String.format("%s/project/%s/service/%s/acl", baseUrl, project, service_name);
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        CreateAclEntryRequest request = new CreateAclEntryRequest();
        CreateAclEntryResponse response = new CreateAclEntryResponse();

        String[] legalPermissions = {"admin", "read", "write", "readwrite"};
        if (Arrays.asList(legalPermissions).contains(permission.toLowerCase())) {
            request.setPermission(permission);
        } else {
            response.setSuccess(false);
            response.setMessage("Illegal permission, must be one of: " + Arrays.toString(legalPermissions));
            return response;
        }
        request.setUsername(username);
        request.setTopic(topic);

        HttpEntity<CreateAclEntryRequest> entity = new HttpEntity<>(request, headers);
        response = restTemplate.postForObject(uri, entity, CreateAclEntryResponse.class);
        return response;
    }
    // TODO: Method to delete ACL

}
