package no.fintlabs.aivenerator.operator;

import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.aivenerator.model.CreateUserResponse;
import no.fintlabs.aivenerator.service.AivenService;
import no.fintlabs.aivenerator.service.SecretService;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ControllerConfiguration
public class AivenUserReconciler implements Reconciler<AivenUserCrd> {

    private final AivenService aivenService;
    private final SecretService secretService;

    public AivenUserReconciler(AivenService aivenService, SecretService secretService) {
        this.aivenService = aivenService;
        this.secretService = secretService;
    }


    @Override
    public UpdateControl<AivenUserCrd> reconcile(AivenUserCrd resource, Context<AivenUserCrd> context) throws Exception {
        log.debug("Reconciling {}", resource.getMetadata().getName());

        // TODO: Fix error
        // java.lang.IllegalArgumentException: There is no event source found for class:io.fabric8.kubernetes.api.model.Secret
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
}
