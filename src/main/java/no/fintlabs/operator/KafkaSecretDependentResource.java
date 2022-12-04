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
import no.fintlabs.aiven.AivenProperties;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.HashMap;
import java.util.Optional;

@Slf4j
@Component
@KubernetesDependent(
        labelSelector = "app.kubernetes.io/managed-by=aivenerator",
        resourceDiscriminator = KafkaSecretDiscriminator.class
)
public class KafkaSecretDependentResource extends FlaisKubernetesDependentResource<Secret, KafkaUserAndAclCrd, KafkaUserAndAclSpec> {

    public static final String NAME_SUFFIX = "-kafka";
    private final AivenProperties aivenProperties;

    public KafkaSecretDependentResource(FlaisWorkflow<KafkaUserAndAclCrd, KafkaUserAndAclSpec> workflow,
                                        KubernetesClient kubernetesClient,
                                        KafkaUserAndAclDependentResource kafkaUserAndAclDependentResource,
                                        AivenProperties aivenProperties,
                                        KafkaSecretDiscriminator discriminator) {
        super(Secret.class, workflow, kubernetesClient);
        this.aivenProperties = aivenProperties;
        setResourceDiscriminator(discriminator);
        dependsOn(kafkaUserAndAclDependentResource);
    }

    @Override
    protected Secret desired(KafkaUserAndAclCrd resource, Context<KafkaUserAndAclCrd> context) {
        log.debug("Desired kafka secret for {}", resource.getMetadata().getName());

        Optional<Secret> thisSecret = context.getSecondaryResources(Secret.class)
                .stream()
                .filter(secret -> secret.getMetadata().getName().equals(getResourceName(resource)))
                .findFirst();
        HashMap<String, String> labels = new HashMap<>(resource.getMetadata().getLabels());

        labels.put("app.kubernetes.io/managed-by", "aivenerator");

        String keyStorePassword = thisSecret.map(secret -> decode(secret.getData().get("spring.kafka.ssl.key-store-password"))).orElse(RandomStringUtils.randomAlphanumeric(32));
        String trustStorePassword = thisSecret.map(secret -> decode(secret.getData().get("spring.kafka.ssl.trust-store-password"))).orElse(RandomStringUtils.randomAlphabetic(32));

        return new SecretBuilder()
                .withNewMetadata()
                .withName(getResourceName(resource))
                .withNamespace(resource.getMetadata().getNamespace())
                .withLabels(labels)
                .endMetadata()
                .withType("Opaque")
                .addToData("fint.kafka.enable-ssl", encode("true"))
                .addToData("spring.kafka.bootstrap-servers", encode(aivenProperties.getKafkaBootstrapServers()))
                .addToData("spring.kafka.ssl.key-password", encode(keyStorePassword))
                .addToData("spring.kafka.ssl.key-store-location", encode("file:/credentials/client.keystore.p12"))
                .addToData("spring.kafka.ssl.key-store-password", encode(keyStorePassword))
                .addToData("spring.kafka.ssl.key-store-type", encode("PKCS12"))
                .addToData("spring.kafka.ssl.protocol", encode("SSL"))
                .addToData("spring.kafka.ssl.trust-store-location", encode("file:/credentials/client.truststore.jks"))
                .addToData("spring.kafka.ssl.trust-store-password", encode(trustStorePassword))
                .addToData("spring.kafka.ssl.trust-store-type", encode("JKS"))
                .build();
    }

    public static String getResourceName(KafkaUserAndAclCrd resource) {
        return resource.getMetadata().getName() + NAME_SUFFIX;
    }

    @Override
    public Matcher.Result<Secret> match(Secret actualResource, KafkaUserAndAclCrd primary, Context<KafkaUserAndAclCrd> context) {
        return super.match(actualResource, primary, context);
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes());
    }

    private String decode(String value) {
        return new String(Base64.getDecoder().decode(value.getBytes()));
    }
}
