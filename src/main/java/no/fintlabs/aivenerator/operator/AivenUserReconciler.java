package no.fintlabs.aivenerator.operator;

import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.Mappers;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.aivenerator.model.CreateUserResponse;
import no.fintlabs.aivenerator.service.AivenService;
import no.fintlabs.aivenerator.service.SecretService;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@ControllerConfiguration
public class AivenUserReconciler implements Reconciler<AivenUserCrd>, EventSourceInitializer<AivenUserCrd>,
        ErrorStatusHandler<AivenUserCrd>,
        Cleaner<AivenUserCrd> {

    private final AivenService aivenService;
    private final SecretService secretService;

    public AivenUserReconciler(AivenService aivenService, SecretService secretService) {
        this.aivenService = aivenService;
        this.secretService = secretService;
    }


    @Override
    public UpdateControl<AivenUserCrd> reconcile(AivenUserCrd resource, Context<AivenUserCrd> context) throws Exception {
        log.debug("Reconciling {}", resource.getMetadata().getName());

        Secret secret = context.getSecondaryResource(Secret.class).orElse(null);
        if (context.getSecondaryResource(Secret.class).isPresent()) {
            log.debug("Secret exists for resource {}", resource.getMetadata().getName());
            return UpdateControl.noUpdate();
        }

        String username = resource.getMetadata().getName() + "_" + uniqId();
        String projectName = resource.getSpec().getProject();
        String serviceName = resource.getSpec().getService();
        CreateUserResponse response = aivenService.createUserForService(projectName, serviceName, username);
        CreateUserResponse.User user = response.getUser();

        secretService.createSecretIfNeeded(context, resource, user.getUsername(), user.getPassword(), user.getAccess_key(), user.getAccess_cert());

        return UpdateControl.updateResourceAndStatus(resource);
    }


    private String uniqId() {
        return RandomStringUtils.randomAlphanumeric(6);
    }

    @Override
    public DeleteControl cleanup(AivenUserCrd resource, Context<AivenUserCrd> context) {
        return null;
    }

    @Override
    public ErrorStatusUpdateControl<AivenUserCrd> updateErrorStatus(AivenUserCrd resource, Context<AivenUserCrd> context, Exception e) {
        AivenUserStatus resourceStatus = resource.getStatus();
        resourceStatus.setErrorMessage(e.getMessage());
        resource.setStatus(resourceStatus);
        return ErrorStatusUpdateControl.updateStatus(resource);
    }

    @Override
    public Map<String, EventSource> prepareEventSources(EventSourceContext<AivenUserCrd> context) {
        return EventSourceInitializer
                .nameEventSources(
                        new InformerEventSource<>(
                                InformerConfiguration.from(Secret.class, context)
                                        .withSecondaryToPrimaryMapper(Mappers.fromOwnerReference())
                                        .build(),
                                context)
                );
    }
}
