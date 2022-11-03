package no.fintlabs.operator;

import io.javaoperatorsdk.operator.api.reconciler.Context;
import no.fintlabs.FlaisExternalDependentResource;
import no.fintlabs.model.CreateAclEntryResponse;
import no.fintlabs.model.CreateUserResponse;
import no.fintlabs.service.AivenService;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

@Component
public class AivenKafkaAclDependentResource extends FlaisExternalDependentResource<AivenKafkaUserAndAcl, AivenKafkaAclCrd, AivenKafkaAclSpec> {

    private final AivenService aivenService;

    public AivenKafkaAclDependentResource(AivenKafkaAclWorkflow workflow, AivenService aivenService) {
        super(AivenKafkaUserAndAcl.class, workflow);
        this.aivenService = aivenService;
        setPollingPeriod(Duration.ofMinutes(10).toMillis());
    }

//    @Override
//    protected AivenKafkaUserAndAcl desired(AivenKafkaAclCrd primary, Context<AivenKafkaAclCrd> context) {
//        CreateUserResponse userResponse = new CreateUserResponse();
//        CreateAclEntryResponse.ACL acl = new CreateAclEntryResponse.ACL();
//        return new AivenKafkaUserAndAcl(userResponse, acl);
//    }

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
        CreateAclEntryResponse.ACL acl = aclResponse.getAcl(username);
        //CreateAclEntryResponse.ACL acl = aclResponse.getAcl()[aclResponse.getAcl().length - 1];

        // TODO: create secret

        return AivenKafkaUserAndAcl.builder()
                .acl(acl)
                .user(response)
                .build();

    }

    @Override
    public Set<AivenKafkaUserAndAcl> fetchResources(AivenKafkaAclCrd primaryResource) {
        // TODO: NOT TESTED
        String projectName = primaryResource.getSpec().getProject();
        String serviceName = primaryResource.getSpec().getService();
        String username = primaryResource.getMetadata().getName();
        AivenKafkaUserAndAcl userAndAcl = aivenService.getUserAndAcl(projectName, serviceName, username);
        return Set.of(userAndAcl);
    }
}
