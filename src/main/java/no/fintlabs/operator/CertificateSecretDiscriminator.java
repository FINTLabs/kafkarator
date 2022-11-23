package no.fintlabs.operator;

import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static no.fintlabs.operator.CertificateSecretDependentResource.NAME_SUFFIX;

@Component
public class CertificateSecretDiscriminator implements ResourceDiscriminator<Secret, KafkaUserAndAclCrd> {
    @Override
    public Optional<Secret> distinguish(Class<Secret> resource, KafkaUserAndAclCrd primary, Context<KafkaUserAndAclCrd> context) {

        InformerEventSource<Secret, KafkaUserAndAclCrd> ies =
                (InformerEventSource<Secret, KafkaUserAndAclCrd>) context
                        .eventSourceRetriever().getResourceEventSourceFor(Secret.class, CertificateSecretDependentResource.class.getSimpleName());

        return ies.get(new ResourceID(CertificateSecretDependentResource.getResourceName(primary)/*primary.getMetadata().getName() + NAME_SUFFIX*/,
                primary.getMetadata().getNamespace()));
    }
}
