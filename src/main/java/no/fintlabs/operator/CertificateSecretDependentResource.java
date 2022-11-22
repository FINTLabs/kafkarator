package no.fintlabs.operator;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.Matcher;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.FlaisKubernetesDependentResource;
import no.fintlabs.FlaisWorkflow;
import no.fintlabs.aiven.AivenService;
import no.fintlabs.keystore.KeyStoreService;
import no.fintlabs.keystore.TrustStoreService;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.HashMap;
import java.util.Optional;

@Slf4j
@Component
@KubernetesDependent(
        labelSelector = "app.kubernetes.io/managed-by=aivenerator"
)
public class CertificateSecretDependentResource extends FlaisKubernetesDependentResource<Secret, KafkaUserAndAclCrd, KafkaUserAndAclSpec> {

    public static final String NAME_SUFFIX = "-kafka-certificates";
    private final AivenService aivenService;
    private final KeyStoreService keyStoreService;
    private final TrustStoreService trustStoreService;

    public CertificateSecretDependentResource(
            FlaisWorkflow<KafkaUserAndAclCrd, KafkaUserAndAclSpec> workflow,
            KubernetesClient kubernetesClient,
            KafkaSecretDependentResource kafkaSecretDependentResource,
            KafkaUserAndAclDependentResource kafkaUserAndAclDependentResource,
            AivenService aivenService,
            KeyStoreService keyStoreService,
            CertificateSecretDiscriminator discriminator, TrustStoreService trustStoreService) {
        super(Secret.class, workflow, kubernetesClient);
        this.aivenService = aivenService;
        this.keyStoreService = keyStoreService;
        this.trustStoreService = trustStoreService;
        dependsOn(kafkaSecretDependentResource, kafkaUserAndAclDependentResource);
        //useEventSourceWithName(this.getClass().getSimpleName());
        setResourceDiscriminator(discriminator);


    }

    @Override
    protected Secret desired(KafkaUserAndAclCrd resource, Context<KafkaUserAndAclCrd> context) {
        log.debug("Desired certificate secret for {}", resource.getMetadata().getName());

        KafkaUserAndAcl kafkaUserAndAcl = context.getSecondaryResource(KafkaUserAndAcl.class).orElseThrow();
        Secret kafkaSecret = context.getSecondaryResources(Secret.class)
                .stream()
                .filter(secret -> secret.getMetadata().getName().equals(resource.getMetadata().getName() + KafkaSecretDependentResource.NAME_SUFFIX))
                .findFirst()
                .orElseThrow();

        Optional<Secret> thisSecret = context.getSecondaryResources(Secret.class)
                .stream()
                .filter(secret -> secret.getMetadata().getName().equals(resource.getMetadata().getName() + NAME_SUFFIX))
                .findFirst();

        String keyStorePassword = decode(kafkaSecret.getData().get("spring.kafka.ssl.key-store-password"));
        String trustStorePassword = kafkaSecret.getData().get("spring.kafka.ssl.trust-store-password");

        String keyStore = thisSecret.map(secret -> secret.getData().get("client.keystore.p12")).orElse(
                keyStoreService.createKeyStoreAndGetAsBase64(
                        kafkaUserAndAcl.getUser().getAccessCert(),
                        kafkaUserAndAcl.getUser().getAccessKey(),
                        aivenService.getCa(),
                        keyStorePassword.toCharArray()
                ));

        String trustStore = thisSecret.map(secret -> secret.getData().get("client.truststore.jks")).orElse(
                trustStoreService.createTrustStoreAndGetAsBase64(
                        aivenService.getCa(),
                        trustStorePassword.toCharArray()
                ));

        HashMap<String, String> labels = new HashMap<>(resource.getMetadata().getLabels());
        labels.put("app.kubernetes.io/managed-by", "aivenerator");


        return new SecretBuilder()
                .withNewMetadata()
                .withName(resource.getMetadata().getName() + NAME_SUFFIX)
                .withNamespace(resource.getMetadata().getNamespace())
                .withLabels(labels)
                .endMetadata()
                .withType("Opaque")
                .addToData("client.keystore.p12", keyStore)
                .addToData("client.truststore.jks", trustStore)
                .build();

    }

    @Override
    public Matcher.Result<Secret> match(Secret actualResource, KafkaUserAndAclCrd primary, Context<KafkaUserAndAclCrd> context) {
        return super.match(actualResource, primary, context);
    }

    private String encode(String value) {

        return Base64.getEncoder().encodeToString(value.getBytes());
    }

    private String decode(String value) {
        return new String(Base64.getDecoder().decode(value.getBytes()));
    }
}
