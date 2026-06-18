package no.fintlabs.operator;

import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ContextInitializer;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Deleter;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.FlaisReconiler;
import no.fintlabs.FlaisWorkflow;
import org.springframework.stereotype.Component;

import java.util.List;

import static no.fintlabs.operator.Constants.*;

@Slf4j
@Component
@ControllerConfiguration
public class KafkaUserAclReconciler extends FlaisReconiler<KafkaUserAndAclCrd, KafkaUserAndAclSpec> implements ContextInitializer<KafkaUserAndAclCrd> {

    public KafkaUserAclReconciler(
            FlaisWorkflow<KafkaUserAndAclCrd, KafkaUserAndAclSpec> workflow,
            List<? extends DependentResource<?, KafkaUserAndAclCrd>> eventSourceProviders,
            List<? extends Deleter<KafkaUserAndAclCrd>> deleters
    ) {
        super(workflow, eventSourceProviders, deleters);
    }

    @Override
    public UpdateControl<KafkaUserAndAclCrd> reconcile(
            KafkaUserAndAclCrd resource,
            Context<KafkaUserAndAclCrd> context
    ) {

        var useNameV2 = context.managedDependentResourceContext().get(SHOULD_USE_NAME_V2, Boolean.class).orElse(false);
        if (useNameV2) {
            resource.getMetadata().getAnnotations().put(NAME_VERSION_ANNOTATION, NAME_VERSION_V2);
        }

        return super.reconcile(resource, context);
    }

    @Override
    public void initContext(KafkaUserAndAclCrd primary, Context<KafkaUserAndAclCrd> context) {
        if (primary.getStatus() == null || primary.getStatus().getObservedGeneration() == null || NameFactory.usesNameVersionV2(primary)) {
            context.managedDependentResourceContext().put(SHOULD_USE_NAME_V2, true);
        }
    }
}
