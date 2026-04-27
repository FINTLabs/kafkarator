package no.fintlabs.operator

import io.fabric8.kubernetes.api.model.Secret
import io.fabric8.kubernetes.api.model.SecretBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import io.javaoperatorsdk.operator.api.reconciler.Context
import no.fintlabs.aiven.AivenProperties
import no.fintlabs.aiven.AivenService
import spock.lang.Specification

class KafkaSecretDependentResourceSpec extends Specification {

    private AivenProperties aivenProperties
    private KafkaSecretDependentResource resource
    private Context<KafkaUserAndAclCrd> context

    def setup() {
        aivenProperties = new AivenProperties(kafkaBootstrapServers: "broker-1:9092,broker-2:9092")
        def workflow = new KafkaUserAndAclWorkflow()
        def kafkaUserAndAclDependentResource = new KafkaUserAndAclDependentResource(
                workflow,
                Mock(AivenService),
                new AivenProperties()
        )
        resource = new KafkaSecretDependentResource(
                workflow,
                Mock(KubernetesClient),
                kafkaUserAndAclDependentResource,
                aivenProperties,
                new KafkaSecretDiscriminator()
        )
        context = Mock()
    }

    def "desired creates secret with generated passwords and expected kafka settings"() {
        given:
        context.getSecondaryResources(Secret.class) >> ([] as Set)

        when:
        def secret = resource.desired(primaryResource(), context)

        then:
        secret.metadata.name == "sample-user-kafka"
        secret.metadata.namespace == "default"
        secret.metadata.labels["app.kubernetes.io/managed-by"] == "kafkarator"
        decode(secret.data["fint.kafka.enable-ssl"]) == "true"
        decode(secret.data["spring.kafka.bootstrap-servers"]) == "broker-1:9092,broker-2:9092"
        decode(secret.data["spring.kafka.ssl.key-store-location"]) == "file:/credentials/client.keystore.p12"
        decode(secret.data["spring.kafka.ssl.trust-store-location"]) == "file:/credentials/client.truststore.jks"
        decode(secret.data["spring.kafka.ssl.key-store-type"]) == "PKCS12"
        decode(secret.data["spring.kafka.ssl.trust-store-type"]) == "JKS"
        decode(secret.data["spring.kafka.ssl.key-password"]) == decode(secret.data["spring.kafka.ssl.key-store-password"])
        decode(secret.data["spring.kafka.ssl.key-store-password"]).size() == 32
        decode(secret.data["spring.kafka.ssl.trust-store-password"]).size() == 32
    }

    def "desired reuses existing passwords when kafka secret already exists"() {
        given:
        def existing = new SecretBuilder()
                .withNewMetadata()
                .withName("sample-user-kafka")
                .withNamespace("default")
                .endMetadata()
                .addToData("spring.kafka.ssl.key-store-password", encode("existing-key-password"))
                .addToData("spring.kafka.ssl.trust-store-password", encode("existing-trust-password"))
                .build()
        context.getSecondaryResources(Secret.class) >> ([existing] as Set)

        when:
        def secret = resource.desired(primaryResource(), context)

        then:
        decode(secret.data["spring.kafka.ssl.key-store-password"]) == "existing-key-password"
        decode(secret.data["spring.kafka.ssl.key-password"]) == "existing-key-password"
        decode(secret.data["spring.kafka.ssl.trust-store-password"]) == "existing-trust-password"
    }

    private static KafkaUserAndAclCrd primaryResource() {
        def primary = new KafkaUserAndAclCrd()
        primary.metadata.name = "sample-user"
        primary.metadata.namespace = "default"
        primary.metadata.labels.put("fintlabs.no/team", "platform")
        primary.metadata.labels.put("fintlabs.no/org-id", "flais.io")
        primary
    }

    private static String encode(String value) {
        Base64.encoder.encodeToString(value.bytes)
    }

    private static String decode(String value) {
        new String(Base64.decoder.decode(value))
    }
}
