package no.fintlabs.aivenerator.operator;

import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Deleter;
import io.javaoperatorsdk.operator.api.reconciler.dependent.EventSourceProvider;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.Mappers;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.FlaisReconiler;
import no.fintlabs.FlaisWorkflow;
import no.fintlabs.aivenerator.model.CreateAclEntryResponse;
import no.fintlabs.aivenerator.model.CreateUserResponse;
import no.fintlabs.aivenerator.service.AivenService;
import no.fintlabs.aivenerator.service.SecretService;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@ControllerConfiguration
public class AivenKafkaAclReconciler extends FlaisReconiler<AivenKafkaAclCrd, AivenKafkaAclSpec>
//        implements
//        Reconciler<AivenKafkaAclCrd>, EventSourceInitializer<AivenKafkaAclCrd>,
//        ErrorStatusHandler<AivenKafkaAclCrd>,
//        Cleaner<AivenKafkaAclCrd>
{
    public AivenKafkaAclReconciler(FlaisWorkflow<AivenKafkaAclCrd, AivenKafkaAclSpec> workflow, List<? extends EventSourceProvider<AivenKafkaAclCrd>> eventSourceProviders, List<? extends Deleter<AivenKafkaAclCrd>> deleters) {
        super(workflow, eventSourceProviders, deleters);
    }

//    private final AivenService aivenService;
//    private final SecretService secretService;
//
//    public AivenKafkaAclReconciler(AivenService aivenService, SecretService secretService) {
//        this.aivenService = aivenService;
//        this.secretService = secretService;
//    }


//    @Override
//    public UpdateControl<AivenKafkaAclCrd> reconcile(AivenKafkaAclCrd resource, Context<AivenKafkaAclCrd> context) throws Exception {
//        log.debug("Reconciling {}", resource.getMetadata().getName());
//        // CrdValidator.validate(resource);
//
//        if (context.getSecondaryResource(Secret.class).isPresent()) {
//            log.debug("Secret exists for resource {}", resource.getMetadata().getName());
//            return UpdateControl.noUpdate();
//        }
//
//        String username = resource.getMetadata().getName() + "_" + uniqId();
//        String projectName = resource.getSpec().getProject();
//        String serviceName = resource.getSpec().getService();
//        String topic = resource.getSpec().getAcl().getTopic();
//        String permission = resource.getSpec().getAcl().getPermission();
//
//        CreateUserResponse response = aivenService.createUserForService(projectName, serviceName, username);
//        CreateUserResponse.User user = response.getUser();
//
//        CreateAclEntryResponse aclResponse = aivenService.createAclEntryForTopic(projectName, serviceName, topic, username, permission);
//        CreateAclEntryResponse.ACL acl = aclResponse.getAcl()[aclResponse.getAcl().length - 1];
//
//        secretService.createSecretIfNeeded(context, resource, user.getUsername(), user.getPassword(), user.getAccess_key(), user.getAccess_cert(), acl.getId());
//
//        return UpdateControl.updateResourceAndStatus(resource);
//    }
//
//
//    private String uniqId() {
//        return RandomStringUtils.randomAlphanumeric(6);
//    }
//
//    @Override
//    public DeleteControl cleanup(AivenKafkaAclCrd resource, Context<AivenKafkaAclCrd> context) {
//        log.debug("Cleaning up {}", resource.getMetadata().getName());
//        String projectName = resource.getSpec().getProject();
//        String serviceName = resource.getSpec().getService();
//
//        String username = secretService.getSecretIfExists(context, resource, resource.getMetadata().getName() + ".aiven.username");
//        aivenService.deleteUserForService(projectName, serviceName, username);
//
//        String aclId = secretService.getSecretIfExists(context, resource, resource.getMetadata().getName() + ".aiven.acl.id");
//        aivenService.deleteAclEntryForService(projectName, serviceName, aclId);
//
//        secretService.deleteSecretIfExists(context);
//        log.info("Cleanup done for {}", resource.getMetadata().getName());
//        return DeleteControl.defaultDelete();
//    }
//
//    @Override
//    public ErrorStatusUpdateControl<AivenKafkaAclCrd> updateErrorStatus(AivenKafkaAclCrd resource, Context<AivenKafkaAclCrd> context, Exception e) {
//        AivenKafkaAclStatus resourceStatus = resource.getStatus();
//        resourceStatus.setErrorMessage(e.getMessage());
//        resource.setStatus(resourceStatus);
//        return ErrorStatusUpdateControl.updateStatus(resource);
//    }
//
//    @Override
//    public Map<String, EventSource> prepareEventSources(EventSourceContext<AivenKafkaAclCrd> context) {
//        return EventSourceInitializer
//                .nameEventSources(
//                        new InformerEventSource<>(
//                                InformerConfiguration.from(Secret.class, context)
//                                        .withSecondaryToPrimaryMapper(Mappers.fromOwnerReference())
//                                        .build(),
//                                context)
//                );
//    }
}
