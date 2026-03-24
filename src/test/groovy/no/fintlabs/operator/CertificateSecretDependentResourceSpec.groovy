package no.fintlabs.operator

import io.fabric8.kubernetes.api.model.Secret
import io.fabric8.kubernetes.api.model.SecretBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import io.javaoperatorsdk.operator.api.reconciler.Context
import no.fintlabs.aiven.AivenProperties
import no.fintlabs.aiven.AivenService
import no.fintlabs.aiven.AivenServiceUser
import no.fintlabs.keystore.KeyStoreService
import no.fintlabs.keystore.TrustStoreService
import spock.lang.Specification

import java.util.Base64

class CertificateSecretDependentResourceSpec extends Specification {

    private AivenService aivenService
    private KeyStoreService keyStoreService
    private TrustStoreService trustStoreService
    private CertificateSecretDependentResource resource
    private Context<KafkaUserAndAclCrd> context

    def setup() {
        aivenService = Mock()
        keyStoreService = Mock()
        trustStoreService = Mock()
        def workflow = new KafkaUserAndAclWorkflow()
        def kafkaUserAndAclDependentResource = new KafkaUserAndAclDependentResource(
                workflow,
                aivenService,
                new AivenProperties()
        )
        def kafkaSecretDependentResource = new KafkaSecretDependentResource(
                workflow,
                Mock(KubernetesClient),
                kafkaUserAndAclDependentResource,
                new AivenProperties(),
                new KafkaSecretDiscriminator()
        )
        resource = new CertificateSecretDependentResource(
                workflow,
                Mock(KubernetesClient),
                kafkaSecretDependentResource,
                kafkaUserAndAclDependentResource,
                aivenService,
                keyStoreService,
                new CertificateSecretDiscriminator(),
                trustStoreService
        )
        context = Mock()
    }

    def "desired creates both stores when certificate secret does not exist"() {
        given:
        def primary = primaryResource()
        def kafkaUserAndAcl = KafkaUserAndAcl.builder()
                .user(AivenServiceUser.builder()
                        .username("resolved-user")
                        .accessCert("client-cert")
                        .accessKey("client-key")
                        .build())
                .build()
        def kafkaSecret = kafkaSecret(primary, "key-pass", "trust-pass")
        context.getSecondaryResource(KafkaUserAndAcl.class) >> Optional.of(kafkaUserAndAcl)
        context.getSecondaryResources(Secret.class) >> ([kafkaSecret] as Set)

        when:
        def secret = resource.desired(primary, context)

        then:
        2 * aivenService.getCa() >> "ca-cert"
        1 * keyStoreService.createKeyStoreAndGetAsBase64("client-cert", "client-key", "ca-cert", {
            new String(it) == "key-pass"
        }) >> "generated-key-store"
        1 * trustStoreService.createTrustStoreAndGetAsBase64("ca-cert", {
            new String(it) == "trust-pass"
        }) >> "generated-trust-store"
        secret.metadata.name == "sample-user-kafka-certificates"
        secret.metadata.labels["app.kubernetes.io/managed-by"] == "kafkarator"
        secret.data["client.keystore.p12"] == "generated-key-store"
        secret.data["client.truststore.jks"] == "generated-trust-store"
    }

    def "desired reuses existing stores when verification succeeds"() {
        given:
        def primary = primaryResource()
        def kafkaUserAndAcl = KafkaUserAndAcl.builder()
                .user(AivenServiceUser.builder()
                        .username("resolved-user")
                        .accessCert("client-cert")
                        .accessKey("client-key")
                        .build())
                .build()
        def kafkaSecret = kafkaSecret(primary, "key-pass", "trust-pass")
        def existingSecret = new SecretBuilder()
                .withNewMetadata()
                .withName("sample-user-kafka-certificates")
                .withNamespace("default")
                .endMetadata()
                .addToData("client.keystore.p12", "existing-key-store")
                .addToData("client.truststore.jks", "existing-trust-store")
                .build()
        context.getSecondaryResource(KafkaUserAndAcl.class) >> Optional.of(kafkaUserAndAcl)
        context.getSecondaryResources(Secret.class) >> ([kafkaSecret, existingSecret] as Set)

        when:
        def secret = resource.desired(primary, context)

        then:
        1 * keyStoreService.verifyKeyStore("existing-key-store", "key-pass") >> "existing-key-store"
        1 * trustStoreService.verifyTrustStore("existing-trust-store", "trust-pass") >> "existing-trust-store"
        0 * keyStoreService.createKeyStoreAndGetAsBase64(_, _, _, _)
        0 * trustStoreService.createTrustStoreAndGetAsBase64(_, _)
        secret.data["client.keystore.p12"] == "existing-key-store"
        secret.data["client.truststore.jks"] == "existing-trust-store"
    }

    def "desired regenerates stores when existing data fails verification"() {
        given:
        def primary = primaryResource()
        def kafkaUserAndAcl = KafkaUserAndAcl.builder()
                .user(AivenServiceUser.builder()
                        .username("resolved-user")
                        .accessCert("client-cert")
                        .accessKey("client-key")
                        .build())
                .build()
        def kafkaSecret = kafkaSecret(primary, "key-pass", "trust-pass")
        def existingSecret = new SecretBuilder()
                .withNewMetadata()
                .withName("sample-user-kafka-certificates")
                .withNamespace("default")
                .endMetadata()
                .addToData("client.keystore.p12", "broken-key-store")
                .addToData("client.truststore.jks", "broken-trust-store")
                .build()
        context.getSecondaryResource(KafkaUserAndAcl.class) >> Optional.of(kafkaUserAndAcl)
        context.getSecondaryResources(Secret.class) >> ([kafkaSecret, existingSecret] as Set)

        when:
        def secret = resource.desired(primary, context)

        then:
        1 * keyStoreService.verifyKeyStore("broken-key-store", "key-pass") >> null
        1 * trustStoreService.verifyTrustStore("broken-trust-store", "trust-pass") >> null
        2 * aivenService.getCa() >> "ca-cert"
        1 * keyStoreService.createKeyStoreAndGetAsBase64("client-cert", "client-key", "ca-cert", {
            new String(it) == "key-pass"
        }) >> "regenerated-key-store"
        1 * trustStoreService.createTrustStoreAndGetAsBase64("ca-cert", {
            new String(it) == "trust-pass"
        }) >> "regenerated-trust-store"
        secret.data["client.keystore.p12"] == "regenerated-key-store"
        secret.data["client.truststore.jks"] == "regenerated-trust-store"
    }

    private static KafkaUserAndAclCrd primaryResource() {
        def primary = new KafkaUserAndAclCrd()
        primary.metadata.name = "sample-user"
        primary.metadata.namespace = "default"
        primary.metadata.labels.put("fintlabs.no/team", "platform")
        primary.metadata.labels.put("fintlabs.no/org-id", "flais.io")
        primary
    }

    private static Secret kafkaSecret(KafkaUserAndAclCrd primary, String keyPassword, String trustPassword) {
        new SecretBuilder()
                .withNewMetadata()
                .withName(KafkaSecretDependentResource.getResourceName(primary))
                .withNamespace(primary.metadata.namespace)
                .endMetadata()
                .addToData("spring.kafka.ssl.key-store-password", encode(keyPassword))
                .addToData("spring.kafka.ssl.trust-store-password", encode(trustPassword))
                .build()
    }

    private static String encode(String value) {
        Base64.encoder.encodeToString(value.bytes)
    }
}
