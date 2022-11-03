package no.fintlabs.service;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.model.CreateAclEntryRequest;
import no.fintlabs.model.CreateAclEntryResponse;
import no.fintlabs.model.CreateUserRequest;
import no.fintlabs.model.CreateUserResponse;
import no.fintlabs.operator.AivenKafkaUserAndAcl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

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

    public Set<AivenKafkaUserAndAcl> getUserAndAcl(String project, String serviceName, String username) {
        String userUrl = baseUrl + "/project/" + project + "/service/" + serviceName + "/user/" + username;
        String aclUrl = baseUrl + "/project/" + project + "/service/" + serviceName + "/acl";
        CreateUserResponse userResponse;
        try {
            userResponse = restTemplate.exchange(userUrl, HttpMethod.GET, new HttpEntity<>(headers), CreateUserResponse.class).getBody();
        } catch (HttpClientErrorException.NotFound e) {
            return Collections.emptySet();
        }
        CreateAclEntryResponse aclResponse = restTemplate.exchange(aclUrl, HttpMethod.GET, new HttpEntity<>(headers), CreateAclEntryResponse.class).getBody();
        CreateAclEntryResponse.ACL acl = null;
        if (aclResponse != null && aclResponse.getAcl() != null) {
            acl = aclResponse.getAclByUsername(username);
        }
        if (userResponse != null && acl != null) {
            return Set.of(new AivenKafkaUserAndAcl(userResponse, acl));
        }
        return Collections.emptySet();
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
        return restTemplate.postForObject(url, entity, CreateUserResponse.class, params);
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

    public String getAclId(String projectName, String serviceName, String username) {
        String aclUrl = baseUrl + "/project/" + projectName + "/service/" + serviceName + "/acl";
        CreateAclEntryResponse aclResponse = restTemplate.exchange(aclUrl, HttpMethod.GET, new HttpEntity<>(headers), CreateAclEntryResponse.class).getBody();

        assert aclResponse != null;
        return Arrays.stream(aclResponse.getAcl())
                .filter(acl -> acl.getUsername().equalsIgnoreCase(username))
                .findFirst()
                .map(CreateAclEntryResponse.ACL::getId)
                .orElse(null);
    }
}
