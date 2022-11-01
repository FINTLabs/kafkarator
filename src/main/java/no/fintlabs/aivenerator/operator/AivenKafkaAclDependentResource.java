package no.fintlabs.aivenerator.operator;

import io.javaoperatorsdk.operator.api.reconciler.Context;
import no.fintlabs.FlaisExternalDependentResource;
import no.fintlabs.FlaisWorkflow;
import no.fintlabs.aivenerator.model.CreateAclEntryResponse;
import no.fintlabs.aivenerator.model.CreateUserResponse;
import no.fintlabs.aivenerator.service.AivenService;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AivenKafkaAclDependentResource extends FlaisExternalDependentResource<AivenKafkaUserAndAcl, AivenKafkaAclCrd, AivenKafkaAclSpec> {

    private AivenService aivenService;

    public AivenKafkaAclDependentResource(Class<AivenKafkaUserAndAcl> resourceType, FlaisWorkflow<AivenKafkaAclCrd, AivenKafkaAclSpec> workflow, AivenService aivenService) {
        super(resourceType, workflow);
        this.aivenService = aivenService;
    }

    @Override
    public void delete(AivenKafkaAclCrd primary, Context<AivenKafkaAclCrd> context) {

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

        return AivenKafkaUserAndAcl.builder()
                .acl(aclResponse)
                .user(response)
                .build();

    }

    @Override
    public Set<AivenKafkaUserAndAcl> fetchResources(AivenKafkaAclCrd primaryResource) {
        aivenService
        return null;
    }
}
