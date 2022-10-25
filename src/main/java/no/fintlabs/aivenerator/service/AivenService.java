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
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class AivenService {

    @Value("${aiven.base_url}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final HttpHeaders headers;

    public AivenService(@Value("${aiven.token}") String token) {
        this.restTemplate = new RestTemplate();
        headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
    }

    public CreateUserResponse createUserForService(String project, String service_name, String username) {
        log.debug("Creating user {} for service {}", username, service_name);
        String url = baseUrl + "/project/{project_name}/service/{service_name}/user";
        Map<String, String> params = new HashMap<>();
        params.put("project_name", project);
        params.put("service_name", service_name);

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername(username);

        HttpEntity<CreateUserRequest> entity = new HttpEntity<>(request, headers);
        CreateUserResponse response = restTemplate.postForObject(url, entity, CreateUserResponse.class, params);
        return response;
    }

    public void deleteUserForService(String project, String service_name, String username) {
        log.debug("Deleting user {} from service {}", username, service_name);
        String url = baseUrl + "/project/{project_name}/service/{service_name}/user/{username}";
        HashMap<String, String> params = new HashMap<>();
        params.put("project_name", project);
        params.put("service_name", service_name);
        params.put("username", username);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class, params);
    }

    public CreateAclEntryResponse createAclEntryForTopic(String project, String service_name, String topic, String username, String permission) {
        log.debug("Creating ACL entry for topic {} for user {} with permission {}", topic, username, permission);
        String url = baseUrl + "/project/{project_name}/service/{service_name}/acl";
        Map<String, String> params = new HashMap<>();
        params.put("project_name", project);
        params.put("service_name", service_name);

        CreateAclEntryRequest request = new CreateAclEntryRequest();
        CreateAclEntryResponse response = new CreateAclEntryResponse();

        String[] legalPermissions = {"admin", "read", "write", "readwrite"};
        if (Arrays.asList(legalPermissions).contains(permission.toLowerCase())) {
            request.setPermission(permission);
        } else {
            response.setMessage("Illegal permission, must be one of: " + Arrays.toString(legalPermissions));
            response.setSuccess(false);
            return response;
        }
        request.setUsername(username);
        request.setTopic(topic);

        HttpEntity<CreateAclEntryRequest> entity = new HttpEntity<>(request, headers);
        response = restTemplate.postForObject(url, entity, CreateAclEntryResponse.class, params);

        assert response != null;
        if (response.getMessage().equalsIgnoreCase("added")) {
            response.setSuccess(true);
        }
        return response;
    }
    // TODO: Method to delete ACL

}
