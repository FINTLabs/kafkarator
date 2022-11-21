package no.fintlabs.aiven;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.operator.KafkaUserAndAcl;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;

@Slf4j
@Component
public class AivenService {

    private final WebClient webClient;
    private final AivenProperties aivenProperties;

    public AivenService(WebClient webClient, AivenProperties aivenProperties) {
        this.webClient = webClient;
        this.aivenProperties = aivenProperties;
    }

    public Optional<KafkaUserAndAcl> getUserAndAcl(String username) {
        try {
            CreateKafkaUserResponse createKafkaUserResponse = webClient.get()
                    .uri("/project/{project_name}/service/{service_name}/user/{username}", aivenProperties.getProject(), aivenProperties.getService(), username)
                    .retrieve()
                    .bodyToMono(CreateKafkaUserResponse.class)
                    .block();

            CreateKafkaAclEntryResponse aclEntryResponse = webClient.get()
                    .uri("/project/{project_name}/service/{service_name}/acl", aivenProperties.getProject(), aivenProperties.getService())
                    .retrieve()
                    .bodyToMono(CreateKafkaAclEntryResponse.class)
                    .block();

            return Optional.ofNullable(KafkaUserAndAcl.fromUserAndAclResponse(createKafkaUserResponse, aclEntryResponse));


        } catch (WebClientResponseException e) {
            log.error("An error occurred when calling endpoint {}. Status code {}", Objects.requireNonNull(e.getRequest()).getURI(), e.getStatusCode());
            return Optional.empty();
        }
    }

    public AivenServiceUser createUserForService(String username)  {
        log.debug("Creating user {} for service {}", username, aivenProperties.getService());

        CreateKafkaUserResponse createKafkaUserResponse = Optional.ofNullable(webClient.post()
                        .uri("/project/{project_name}/service/{service_name}/user", aivenProperties.getProject(), aivenProperties.getService())
                        .body(BodyInserters.fromValue(new CreateKafkaUserRequest(username)))
                        .retrieve()
                        .bodyToMono(CreateKafkaUserResponse.class)
                        .block())
                .orElseThrow();

        log.debug("Created Aiven Kafka service user with message: {}", Objects.requireNonNull(createKafkaUserResponse).getMessage());

        return createKafkaUserResponse.getUser();
    }

    public void deleteUserForService( String username) {
        log.debug("Deleting user {} from service {}", username, aivenProperties.getService());

        webClient
                .delete()
                .uri("/project/{project_name}/service/{service_name}/user/{username}", aivenProperties.getProject(), aivenProperties.getService(), username)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    public KafkaAclEntry createAclEntryForTopic(KafkaAclEntry aclEntry) {
        log.debug("Creating ACL entry for topic {} for user {} with permission {}", aclEntry.getTopic(), aclEntry.getUsername(), aclEntry.getPermission());

        validatePermission(aclEntry.getPermission());

        return webClient
                .post()
                .uri("/project/{project_name}/service/{service_name}/acl", aivenProperties.getProject(), aivenProperties.getService())
                .body(BodyInserters.fromValue(
                        CreateKafkaAclEntryRequest
                                .builder()
                                .topic(aclEntry.getTopic())
                                .permission(aclEntry.getPermission())
                                .username(aclEntry.getUsername())
                                .build()
                ))
                .retrieve()
                .bodyToMono(CreateKafkaAclEntryResponse.class)
                .block().getAclByUsernameAndTopic(aclEntry.getUsername(), aclEntry.getTopic());
    }

    public void deleteAclEntryForService( String aclId) {
        log.debug("Deleting ACL entry for service {}", aivenProperties.getService());

        webClient
                .delete()
                .uri("/project/{project_name}/service/{service_name}/acl/{acl_id}", aivenProperties.getProject(), aivenProperties.getService(), aclId)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    public KafkaUserAndAcl updateAclEntries(KafkaUserAndAcl actual, KafkaUserAndAcl desired) {
        Collection<KafkaAclEntry> aclEntriesToRemove = CollectionUtils.removeAll(actual.getAclEntries(), desired.getAclEntries());
        Collection<KafkaAclEntry> aclEntriesToAdd = CollectionUtils.removeAll(desired.getAclEntries(), actual.getAclEntries());

        aclEntriesToRemove.forEach(aclEntry -> deleteAclEntryForService(aclEntry.getId()));
        aclEntriesToAdd.forEach(this::createAclEntryForTopic);
        return getUserAndAcl(desired.getUser().getUsername()).orElseThrow();
    }

    public String getCaCert() {

        return Objects.requireNonNull(webClient
                        .get()
                        .uri("/project/{project_name}/kms/ca", aivenProperties.getProject())
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
