package no.fintlabs.aiven;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.operator.KafkaUserAndAcl;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.annotation.PostConstruct;
import java.util.*;

@Slf4j
@Component
public class AivenService {

    private final WebClient webClient;
    private final AivenProperties aivenProperties;

    @Getter
    private String ca;

    public AivenService(WebClient webClient, AivenProperties aivenProperties) {
        this.webClient = webClient;
        this.aivenProperties = aivenProperties;
    }

    @PostConstruct
    public void init() {
        ca = getCaCert();
    }

    public Optional<KafkaUserAndAcl> getUserAndAcl(String username) {
        try {

            GetKafkaUserResponse getKafkaUserResponse = webClient.get()
                    .uri(
                            "/project/{project_name}/service/{service_name}/user/{username}",
                            aivenProperties.getProject(),
                            aivenProperties.getService(),
                            username
                    )
                    .retrieve()
                    .bodyToMono(GetKafkaUserResponse.class)
                    .block();

            GetKafkaAclEntryResponse aclEntryResponse = webClient.get()
                    .uri(
                            "/project/{project_name}/service/{service_name}/acl",
                            aivenProperties.getProject(),
                            aivenProperties.getService()
                    )
                    .retrieve()
                    .bodyToMono(GetKafkaAclEntryResponse.class)
                    .block();

            if (getKafkaUserResponse == null || getKafkaUserResponse.getUser() == null || aclEntryResponse == null) {
                return Optional.empty();
            }

            return Optional.of(
                    KafkaUserAndAcl.fromUserAndAclResponse(getKafkaUserResponse, aclEntryResponse)
            );

        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.debug("Could not find user {}. Proceeding to create...", username);
                return Optional.empty();
            } else {
                log.error("Error fetching user or ACL for {}: {}", username, e.getMessage());
                throw e;
            }
        }
    }

    public AivenServiceUser ensureUserForService(String username) {
        return getUserAndAcl(username)
                .map(KafkaUserAndAcl::getUser)
                .orElseGet(() -> createUserForService(username));
    }

    public AivenServiceUser createUserForService(String username) {
        log.debug("Creating user {} for service {}", username, aivenProperties.getService());
        try {

            CreateKafkaUserResponse createKafkaUserResponse = Optional.ofNullable(webClient.post()
                            .uri(
                                    "/project/{project_name}/service/{service_name}/user",
                                    aivenProperties.getProject(),
                                    aivenProperties.getService()
                            )
                            .body(BodyInserters.fromValue(new CreateKafkaUserRequest(username)))
                            .retrieve()
                            .bodyToMono(CreateKafkaUserResponse.class)
                            .block())
                    .orElseThrow();

            log.debug(
                    "Created Aiven Kafka service user with message: {}",
                    Objects.requireNonNull(createKafkaUserResponse).getMessage()
            );

            return createKafkaUserResponse.getUser();

        } catch (WebClientResponseException exception) {
            if (exception.getStatusCode() == HttpStatus.CONFLICT) {
                log.debug("User {} already exists. Fetching user...", username);

                return getUserAndAcl(username)
                        .map(KafkaUserAndAcl::getUser)
                        .orElseThrow(() -> new IllegalStateException(
                                "User already exists, but could not be fetched: " + username
                        ));
            }

            throw exception;
        }
    }

    public void deleteUserForService(String username) {
        log.debug("Deleting user {} from service {}", username, aivenProperties.getService());

        webClient
                .delete()
                .uri("/project/{project_name}/service/{service_name}/user/{username}", aivenProperties.getProject(), aivenProperties.getService(), username)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    public KafkaAclEntry ensureAclEntryForTopic(KafkaAclEntry desiredAclEntry) {
        return getUserAndAcl(desiredAclEntry.getUsername())
                .flatMap(userAndAcl -> userAndAcl.getAclEntries()
                        .stream()
                        .filter(existingAclEntry ->
                                Objects.equals(existingAclEntry.getTopic(), desiredAclEntry.getTopic())
                                        && Objects.equals(existingAclEntry.getPermission(), desiredAclEntry.getPermission())
                        )
                        .findFirst()
                )
                .orElseGet(() -> createAclEntryForTopic(desiredAclEntry));
    }

    public KafkaAclEntry createAclEntryForTopic(KafkaAclEntry aclEntry) {
        log.debug("Creating ACL entry for topic {} for user {} with permission {}", aclEntry.getTopic(), aclEntry.getUsername(), aclEntry.getPermission());

        validatePermission(aclEntry.getPermission());

        try {
            CreateKafkaAclEntryResponse response = webClient
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
                    .block();

            return Optional.ofNullable(Objects.requireNonNull(response)
                            .getAclByUsernameAndTopic(aclEntry.getUsername(), aclEntry.getTopic()))
                    .orElseThrow(() -> new IllegalStateException(
                            "Created ACL, but could not find it in response for user "
                                    + aclEntry.getUsername()
                                    + " and topic "
                                    + aclEntry.getTopic()
                    ));

        } catch (WebClientResponseException exception) {
            if (exception.getStatusCode() == HttpStatus.CONFLICT) {
                log.debug("ACL already exists. Fetching existing ACL for user {} and topic {}", aclEntry.getUsername(), aclEntry.getTopic());

                return getUserAndAcl(aclEntry.getUsername())
                        .flatMap(userAndAcl -> userAndAcl.getAclEntries()
                                .stream()
                                .filter(existingAclEntry ->
                                        Objects.equals(existingAclEntry.getTopic(), aclEntry.getTopic())
                                                && Objects.equals(existingAclEntry.getPermission(), aclEntry.getPermission())
                                )
                                .findFirst()
                        )
                        .orElseThrow(() -> new IllegalStateException(
                                "ACL already exists, but could not be fetched for user "
                                        + aclEntry.getUsername()
                                        + " and topic "
                                        + aclEntry.getTopic()
                        ));
            }

            throw exception;
        }
    }

    public void deleteAclEntryForService(String aclId) {
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
        aclEntriesToAdd.forEach(this::ensureAclEntryForTopic);
        return getUserAndAcl(desired.getUser().getUsername()).orElseThrow();
    }

    private String getCaCert() {

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
