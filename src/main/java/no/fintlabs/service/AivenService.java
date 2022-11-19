package no.fintlabs.service;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.model.*;
import no.fintlabs.operator.AivenKafkaUserAndAcl;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AivenService {

    private final WebClient webClient;

    public AivenService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Set<AivenKafkaUserAndAcl> getUserAndAcl(String project, String serviceName, String username, List<String> topics) {
        try {
            CreateUserResponse createUserResponse = webClient.get()
                    .uri("/project/{project_name}/service/{service_name}/user/{username}", project, serviceName, username)
                    .retrieve()
                    .bodyToMono(CreateUserResponse.class)
                    .block();

            CreateAclEntryResponse aclEntryResponse = webClient.get()
                    .uri("/project/{project_name}/service/{service_name}/acl", project, serviceName)
                    .retrieve()
                    .bodyToMono(CreateAclEntryResponse.class)
                    .block();

            return Set.of(new AivenKafkaUserAndAcl(createUserResponse, topics.stream()
                    .map(topic -> Objects.requireNonNull(aclEntryResponse).getAclByUsernameAndTopic(username, topic))
                    .collect(Collectors.toList())
            ));


        } catch (WebClientResponseException e) {
            log.error("An error occurred when calling endpoint {}. Status code {}", Objects.requireNonNull(e.getRequest()).getURI(), e.getStatusCode());
            return Collections.emptySet();
        }
    }

    public CreateUserResponse createUserForService(String project, String serviceName, String username) {
        log.debug("Creating user {} for service {}", username, serviceName);

        return webClient.post()
                .uri("/project/{project_name}/service/{service_name}/user", project, serviceName)
                .body(BodyInserters.fromValue(new CreateUserRequest(username)))
                .retrieve()
                .bodyToMono(CreateUserResponse.class)
                .block();
    }

    public void deleteUserForService(String project, String serviceName, String username) {
        log.debug("Deleting user {} from service {}", username, serviceName);

        webClient
                .delete()
                .uri("/project/{project_name}/service/{service_name}/user/{username}", project, serviceName, username)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    public CreateAclEntryResponse createAclEntryForTopic(String project, String serviceName, String topic, String username, String permission) {
        log.debug("Creating ACL entry for topic {} for user {} with permission {}", topic, username, permission);

        validatePermission(permission);

        return webClient
                .post()
                .uri("/project/{project_name}/service/{service_name}/acl", project, serviceName)
                .body(BodyInserters.fromValue(
                        CreateAclEntryRequest
                                .builder()
                                .topic(topic)
                                .permission(permission)
                                .username(username)
                                .build()
                ))
                .retrieve()
                .bodyToMono(CreateAclEntryResponse.class)
                .block();
    }

    public void deleteAclEntryForService(String project, String serviceName, String aclId) {
        log.debug("Deleting ACL entry for service {}", serviceName);

        webClient
                .delete()
                .uri("/project/{project_name}/service/{service_name}/acl/{acl_id}", project, serviceName, aclId)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    public String getAclId(String projectName, String serviceName, String username, String topic) {

        return Optional.ofNullable(webClient
                        .get()
                        .uri("/project/{project_name}/service/{service_name}/acl", projectName, serviceName)
                        .retrieve()
                        .bodyToMono(CreateAclEntryResponse.class)
                        .block())
                .map(createAclEntryResponse -> createAclEntryResponse.getAclByUsernameAndTopic(username, topic))
                .map(Acl::getId)
                .orElse(null);
    }

    public String getCaCert(String projectName) {

        return Objects.requireNonNull(webClient
                        .get()
                        .uri("/project/{project_name}/kms/ca", projectName)
                        .retrieve()
                        .bodyToMono(CaCertResponse.class)
                        .block())
                .getCertificate();
    }

    private void validatePermission(String permission) {
        List<String> legalPermissions = Arrays.asList("admin", "read", "write", "readwrite");

        if (!legalPermissions.contains(permission.toLowerCase())) {
            throw new IllegalArgumentException(permission + " is not a valid Kafka ACL permission");
        }
    }
}
