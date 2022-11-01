package no.fintlabs.aivenerator.operator;

import io.javaoperatorsdk.operator.api.reconciler.Context;
import no.fintlabs.FlaisExternalDependentResource;
import no.fintlabs.FlaisWorkflow;
import no.fintlabs.aivenerator.model.CreateAclEntryResponse;
import no.fintlabs.aivenerator.model.CreateUserResponse;
import no.fintlabs.aivenerator.service.AivenService;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

@Component
public class AivenKafkaAclDependentResource extends FlaisExternalDependentResource<AivenKafkaUserAndAcl, AivenKafkaAclCrd, AivenKafkaAclSpec> {

    private AivenService aivenService;

    public AivenKafkaAclDependentResource(Class<AivenKafkaUserAndAcl> resourceType, FlaisWorkflow<AivenKafkaAclCrd, AivenKafkaAclSpec> workflow, AivenService aivenService) {
        super(resourceType, workflow);
        this.aivenService = aivenService;
        setPollingPeriod(Duration.ofMinutes(10).toMillis());
    }

    @Override
    public void delete(AivenKafkaAclCrd primary, Context<AivenKafkaAclCrd> context) {
        // TODO: NOT TESTED
        String projectName = primary.getSpec().getProject();
        String serviceName = primary.getSpec().getService();
        String username = primary.getMetadata().getName();

        String aclId = aivenService.getAclId(projectName, serviceName, username);

        aivenService.deleteUserForService(projectName, serviceName, username);
        aivenService.deleteAclEntryForService(projectName, serviceName, aclId);

        // TODO: delete secret

    }

    @Override
    public AivenKafkaUserAndAcl create(AivenKafkaUserAndAcl desired, AivenKafkaAclCrd primary, Context<AivenKafkaAclCrd> context) {
        String projectName = primary.getSpec().getProject();
        String serviceName = primary.getSpec().getService();
        String username = primary.getMetadata().getName();
        String topic = primary.getSpec().getAcl().getTopic();
        String permission = primary.getSpec().getAcl().getPermission();

        CreateUserResponse response = aivenService.createUserForService(projectName, serviceName, username);
        //CreateUserResponse.User user = response.getUser();

        CreateAclEntryResponse aclResponse = aivenService.createAclEntryForTopic(projectName, serviceName, topic, username, permission);
        //CreateAclEntryResponse.ACL acl = aclResponse.getAcl()[aclResponse.getAcl().length - 1];

        // TODO: create secret

        return AivenKafkaUserAndAcl.builder()
                .acl(aclResponse)
                .user(response)
                .build();

    }

    @Override
    public Set<AivenKafkaUserAndAcl> fetchResources(AivenKafkaAclCrd primaryResource) {
        // TODO: NOT TESTED, NOT FINISHED
        String projectName = "";
        String serviceName = "";
        String username = "";
        aivenService.getUserAndAcl(projectName, serviceName, username);
        return null;
    }
}
