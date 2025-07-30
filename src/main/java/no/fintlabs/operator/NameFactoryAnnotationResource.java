package no.fintlabs.operator;

import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Deleter;
import io.javaoperatorsdk.operator.api.reconciler.dependent.ReconcileResult;
import io.javaoperatorsdk.operator.processing.dependent.AbstractDependentResource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class NameFactoryAnnotationResource extends AbstractDependentResource<Void, KafkaUserAndAclCrd> implements Deleter<KafkaUserAndAclCrd> {

    public NameFactoryAnnotationResource() {
        super();
    }

    @Override
    public Class<Void> resourceType() {
        return Void.class;
    }

    @Override
    public Void desired(KafkaUserAndAclCrd resource, Context<KafkaUserAndAclCrd> context) {
        return null;
    }

    @Override
    public ReconcileResult<Void> reconcile(KafkaUserAndAclCrd parent, Context<KafkaUserAndAclCrd> context) {
        String generatedUsername = NameFactory.nameFromMetadata(parent);

        Map<String, String> annotations = Optional.ofNullable(parent.getMetadata().getAnnotations())
                .orElse(new HashMap<>());
        annotations.put("fintlabs.no/generated-username", generatedUsername);
        parent.getMetadata().setAnnotations(annotations);

        return ReconcileResult.noOperation(null);
    }

    @Override
    public void onCreated(KafkaUserAndAclCrd parent, Void resource, Context<KafkaUserAndAclCrd> context) {
        // No-op
    }

    @Override
    public void onUpdated(KafkaUserAndAclCrd parent, Void actualResource, Void desiredResource, Context<KafkaUserAndAclCrd> context) {
        // No-op
    }
}


