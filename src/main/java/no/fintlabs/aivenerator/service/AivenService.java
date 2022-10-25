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

    public CreateUserResponse createUserForService(String project, String serviceName, String username) {
        log.debug("Creating user {} for service {}", username, serviceName);
        String url = baseUrl + "/project/{project_name}/service/{service_name}/user";
        Map<String, String> params = new HashMap<>();
        params.put("project_name", project);
        params.put("service_name", serviceName);

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername(username);

        HttpEntity<CreateUserRequest> entity = new HttpEntity<>(request, headers);
        CreateUserResponse response = restTemplate.postForObject(url, entity, CreateUserResponse.class, params);
        return response;
    }

    public void deleteUserForService(String project, String serviceName, String username) {
        log.debug("Deleting user {} from service {}", username, serviceName);
        String url = baseUrl + "/project/{project_name}/service/{service_name}/user/{username}";
        HashMap<String, String> params = new HashMap<>();
        params.put("project_name", project);
        params.put("service_name", serviceName);
        params.put("username", username);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class, params);
    }

    public CreateAclEntryResponse createAclEntryForTopic(String project, String serviceName, String topic, String username, String permission) {
        log.debug("Creating ACL entry for topic {} for user {} with permission {}", topic, username, permission);
        String url = baseUrl + "/project/{project_name}/service/{service_name}/acl";
        Map<String, String> params = new HashMap<>();
        params.put("project_name", project);
        params.put("service_name", serviceName);

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

    public void deleteAclEntryForService(String project, String serviceName, String aclId) {
        log.debug("Deleting ACL entry for service {}", serviceName);
        String url = baseUrl + "/project/{project_name}/service/{service_name}/acl/{acl_id}";
        HashMap<String, String> params = new HashMap<>();
        params.put("project_name", project);
        params.put("service_name", serviceName);
        params.put("acl_id", aclId);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class, params);
    }
}
