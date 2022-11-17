package no.fintlabs.operator;

import io.javaoperatorsdk.operator.api.reconciler.Context;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.FlaisExternalDependentResource;
import no.fintlabs.model.Acl;
import no.fintlabs.model.CreateAclEntryResponse;
import no.fintlabs.model.CreateUserResponse;
import no.fintlabs.service.AivenService;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
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
        CreateUserResponse userResponse = new CreateUserResponse();
        List<Acl> acls = new ArrayList<>();
        return AivenKafkaUserAndAcl.builder()
                .user(userResponse)
                .acls(acls)
                .build();
    }

    @Override
    public void delete(AivenKafkaAclCrd primary, Context<AivenKafkaAclCrd> context) {
        String projectName = primary.getSpec().getProject();
        String serviceName = primary.getSpec().getService();
        String username = primary.getMetadata().getName();

        for (AivenKafkaAclSpec.Acl acl : primary.getSpec().getAcls()) {
            String topic = acl.getTopic();
            String aclId = aivenService.getAclId(projectName, serviceName, username, topic);
            aivenService.deleteAclEntryForService(projectName, serviceName, aclId);
        }
        aivenService.deleteUserForService(projectName, serviceName, username);
    }

    @Override
    public AivenKafkaUserAndAcl create(AivenKafkaUserAndAcl desired, AivenKafkaAclCrd primary, Context<AivenKafkaAclCrd> context) {
        String projectName = primary.getSpec().getProject();
        String serviceName = primary.getSpec().getService();
        String username = primary.getMetadata().getName();
        List<AivenKafkaAclSpec.Acl> acls = primary.getSpec().getAcls();
        List<Acl> aclList = new ArrayList<>();
        for (int i = 0; i < acls.size(); i++) {
            String topic = primary.getSpec().getAcls().get(i).getTopic();
            String permission = primary.getSpec().getAcls().get(i).getPermission();
            CreateAclEntryResponse aclEntryResponse = aivenService.createAclEntryForTopic(projectName, serviceName, topic, username, permission);
            log.debug("ACL entry created: " + aclEntryResponse);
            aclList.add(aclEntryResponse.getAclByUsernameAndTopic(username, topic));
        }

        CreateUserResponse response = aivenService.createUserForService(projectName, serviceName, username);

        return AivenKafkaUserAndAcl.builder()
                .acls(aclList)
                .user(response)
                .build();

    }


    @Override
    public Set<AivenKafkaUserAndAcl> fetchResources(AivenKafkaAclCrd primaryResource) {
        String projectName = primaryResource.getSpec().getProject();
        String serviceName = primaryResource.getSpec().getService();
        String username = primaryResource.getMetadata().getName();
        List<String> topics = new ArrayList<>();
        for (int i = 0; i < primaryResource.getSpec().getAcls().size(); i++) {
            topics.add(primaryResource.getSpec().getAcls().get(i).getTopic());
        }
        return aivenService.getUserAndAcl(projectName, serviceName, username, topics);
    }
}
