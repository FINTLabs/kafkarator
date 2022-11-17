package no.fintlabs.operator;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.FlaisKubernetesDependentResource;
import no.fintlabs.FlaisWorkflow;
import no.fintlabs.model.Acl;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Optional;

@Slf4j
@Component
public class AivenKafKaAclSecretDependentResource extends FlaisKubernetesDependentResource<Secret, AivenKafkaAclCrd, AivenKafkaAclSpec> {
    public AivenKafKaAclSecretDependentResource(FlaisWorkflow<AivenKafkaAclCrd, AivenKafkaAclSpec> workflow, KubernetesClient kubernetesClient, AivenKafkaAclDependentResource aivenKafkaAclDependentResource) {
        super(Secret.class, workflow, kubernetesClient);
        dependsOn(aivenKafkaAclDependentResource);
    }

    @Override
    protected Secret desired(AivenKafkaAclCrd resource, Context<AivenKafkaAclCrd> context) {
        log.debug("Desired secret for {}", resource.getMetadata().getName());

        Optional<AivenKafkaUserAndAcl> userAndAcl = context.getSecondaryResource(AivenKafkaUserAndAcl.class);
        AivenKafkaUserAndAcl aivenKafkaUserAndAcl = userAndAcl.orElseThrow();

        HashMap<String, String> labels = new HashMap<>(resource.getMetadata().getLabels());

        labels.put("app.kubernetes.io/managed-by", "aivenerator");
        return new SecretBuilder().withNewMetadata().withName(resource.getMetadata().getName()).withNamespace(resource.getMetadata().getNamespace()).withLabels(labels).endMetadata().withStringData(new HashMap<>() {{
            // TODO: update keys
            put(resource.getMetadata().getName() + ".aiven.username", aivenKafkaUserAndAcl.getUser().getUser().getUsername());
            put(resource.getMetadata().getName() + ".aiven.password", aivenKafkaUserAndAcl.getUser().getUser().getPassword());
            put(resource.getMetadata().getName() + ".aiven.access.key", aivenKafkaUserAndAcl.getUser().getUser().getAccess_key());
            put(resource.getMetadata().getName() + ".aiven.access.cert", aivenKafkaUserAndAcl.getUser().getUser().getAccess_cert());
            for (Acl acl : aivenKafkaUserAndAcl.getAcls()) {
                put(resource.getMetadata().getName() + ".aiven.acl." + acl.getId(), acl.getId());
            }
        }}).build();
    }
}
