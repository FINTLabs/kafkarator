package no.fintlabs.operator;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.FlaisKubernetesDependentResource;
import no.fintlabs.FlaisWorkflow;
import no.fintlabs.model.KafkaAclEntry;
import no.fintlabs.service.AivenService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Optional;

@Slf4j
@Component
public class AivenKafKaAclSecretDependentResource extends FlaisKubernetesDependentResource<Secret, AivenKafkaAclCrd, AivenKafkaAclSpec> {

    public AivenKafKaAclSecretDependentResource(FlaisWorkflow<AivenKafkaAclCrd, AivenKafkaAclSpec> workflow, KubernetesClient kubernetesClient, AivenKafkaAclDependentResource aivenKafkaAclDependentResource, AivenService aivenService) {
        super(Secret.class, workflow, kubernetesClient);
        dependsOn(aivenKafkaAclDependentResource);
    }

    @Override
    protected Secret desired(AivenKafkaAclCrd resource, Context<AivenKafkaAclCrd> context) {
        log.debug("Desired secret for {}", resource.getMetadata().getName());

        Optional<KafkaUserAndAcl> userAndAcl = context.getSecondaryResource(KafkaUserAndAcl.class);
        KafkaUserAndAcl kafkaUserAndAcl = userAndAcl.orElseThrow();

        HashMap<String, String> labels = new HashMap<>(resource.getMetadata().getLabels());

        labels.put("app.kubernetes.io/managed-by", "aivenerator");


        //String keystoreString = aivenService.createKeyStore(aivenKafkaUserAndAcl.getUser().getUser().getAccess_cert(), aivenKafkaUserAndAcl.getUser().getUser().getAccess_key()).toString();
        //String caCertString = aivenService.getCaCert(resource.getSpec().getProject());
        return new SecretBuilder()
                .withNewMetadata()
                .withName(resource.getMetadata().getName())
                .withNamespace(resource.getMetadata().getNamespace())
                .withLabels(labels)
                .endMetadata()
                .withStringData(new HashMap<>() {{
                    // TODO: update keys
//                    put(resource.getMetadata().getName() + ".aiven.username", kafkaUserAndAcl.getUser().getUser().getUsername());
//                    put(resource.getMetadata().getName() + ".aiven.password", kafkaUserAndAcl.getUser().getUser().getPassword());
//                    put(resource.getMetadata().getName() + ".aiven.access.key", kafkaUserAndAcl.getUser().getUser().getAccess_key());
//                    put(resource.getMetadata().getName() + ".aiven.access.cert", kafkaUserAndAcl.getUser().getUser().getAccess_cert());
                    for (KafkaAclEntry kafkaAclEntry : kafkaUserAndAcl.getAclEntries()) {
                        put(resource.getMetadata().getName() + ".aiven.acl." + kafkaAclEntry.getId(), kafkaAclEntry.getId());
                    }
                }}).build();
    }
}
