package no.fintlabs.operator;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.FlaisKubernetesDependentResource;
import no.fintlabs.FlaisWorkflow;
import no.fintlabs.model.Acl;
import no.fintlabs.service.AivenService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Optional;

@Slf4j
@Component
public class AivenKafKaAclSecretDependentResource extends FlaisKubernetesDependentResource<Secret, AivenKafkaAclCrd, AivenKafkaAclSpec> {

    private final AivenService aivenService;

    public AivenKafKaAclSecretDependentResource(FlaisWorkflow<AivenKafkaAclCrd, AivenKafkaAclSpec> workflow, KubernetesClient kubernetesClient, AivenKafkaAclDependentResource aivenKafkaAclDependentResource, AivenService aivenService) {
        super(Secret.class, workflow, kubernetesClient);
        this.aivenService = aivenService;
        dependsOn(aivenKafkaAclDependentResource);
    }

    @Override
    protected Secret desired(AivenKafkaAclCrd resource, Context<AivenKafkaAclCrd> context) {
        log.debug("Desired secret for {}", resource.getMetadata().getName());

        Optional<AivenKafkaUserAndAcl> userAndAcl = context.getSecondaryResource(AivenKafkaUserAndAcl.class);
        AivenKafkaUserAndAcl aivenKafkaUserAndAcl = userAndAcl.orElseThrow();

        HashMap<String, String> labels = new HashMap<>(resource.getMetadata().getLabels());

        labels.put("app.kubernetes.io/managed-by", "aivenerator");


        //String keystoreString = aivenService.createKeyStore(aivenKafkaUserAndAcl.getUser().getUser().getAccess_cert(), aivenKafkaUserAndAcl.getUser().getUser().getAccess_key()).toString();
        //String caCertString = aivenService.getCaCert(resource.getSpec().getProject());
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
