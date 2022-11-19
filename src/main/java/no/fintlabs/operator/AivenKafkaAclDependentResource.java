package no.fintlabs.operator;

import io.javaoperatorsdk.operator.api.reconciler.Context;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.FlaisExternalDependentResource;
import no.fintlabs.model.Acl;
import no.fintlabs.model.CreateUserResponse;
import no.fintlabs.service.AivenService;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class AivenKafkaAclDependentResource extends FlaisExternalDependentResource<AivenKafkaUserAndAcl, AivenKafkaAclCrd, AivenKafkaAclSpec> {

    private final AivenService aivenService;

    public AivenKafkaAclDependentResource(AivenKafkaAclWorkflow workflow, AivenService aivenService) {
        super(AivenKafkaUserAndAcl.class, workflow);
        this.aivenService = aivenService;
        setPollingPeriod(Duration.ofMinutes(10).toMillis());
    }

    @Override
    protected AivenKafkaUserAndAcl desired(AivenKafkaAclCrd primary, Context<AivenKafkaAclCrd> context) {
        // TODO: 19/11/2022 At the moment this is not representing the desired state
        return AivenKafkaUserAndAcl.builder()
                .build();
    }

    @Override
    public void delete(AivenKafkaAclCrd primary, Context<AivenKafkaAclCrd> context) {
        String projectName = primary.getSpec().getProject();
        String serviceName = primary.getSpec().getService();
        String username = primary.getMetadata().getName();

        primary.getSpec().getAcls()
                .forEach(acl -> {
                    String topic = acl.getTopic();
                    String aclId = aivenService.getAclId(projectName, serviceName, username, topic);
                    aivenService.deleteAclEntryForService(projectName, serviceName, aclId);
                    log.debug("Deleted acl {} for user {} and topic {}", aclId, username, topic);
                });

        aivenService.deleteUserForService(projectName, serviceName, username);
        log.debug("Deleted user {} for service {}", username, serviceName);
    }

    @Override
    public AivenKafkaUserAndAcl create(AivenKafkaUserAndAcl desired, AivenKafkaAclCrd primary, Context<AivenKafkaAclCrd> context) {
        String projectName = primary.getSpec().getProject();
        String serviceName = primary.getSpec().getService();
        String username = primary.getMetadata().getName();

        CreateUserResponse createUserResponse = aivenService.createUserForService(projectName, serviceName, username);
        log.debug("Created user {} for service {}", username, serviceName);

        List<Acl> createdAcls = primary.getSpec().getAcls().stream()
                .map(acl -> {
                    String topic = acl.getTopic();
                    String permission = acl.getPermission();
                    log.debug("Created ACL for user {} on topic {}", username, topic);
                    return aivenService.createAclEntryForTopic(projectName, serviceName, topic, username, permission).getAclByUsernameAndTopic(username, topic);
                }).toList();


        return AivenKafkaUserAndAcl.builder()
                .acls(createdAcls)
                .user(createUserResponse)
                .build();

    }


    @Override
    public Set<AivenKafkaUserAndAcl> fetchResources(AivenKafkaAclCrd primaryResource) {

        return aivenService.getUserAndAcl(primaryResource.getSpec().getProject(),
                primaryResource.getSpec().getService(),
                primaryResource.getMetadata().getName(),
                primaryResource.getSpec().getAcls()
                        .stream()
                        .map(AivenKafkaAclSpec.Acl::getTopic)
                        .toList());
    }
}
