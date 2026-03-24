package no.fintlabs.operator;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.Matcher;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResourceConfig;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.FlaisKubernetesDependentResource;
import no.fintlabs.FlaisWorkflow;
import no.fintlabs.aiven.AivenProperties;
import no.fintlabs.aiven.AivenService;
import no.fintlabs.keystore.KeyStoreService;
import no.fintlabs.keystore.TrustStoreService;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.micrometer.core.instrument.Timer.Sample;

@Slf4j
@Component
public class CertificateSecretDependentResource extends FlaisKubernetesDependentResource<Secret, KafkaUserAndAclCrd, KafkaUserAndAclSpec> {

    public static final String NAME_SUFFIX = "-kafka-certificates";
    public static final String CERTIFICATE_NOT_AFTER_ANNOTATION = "kafkarator.fintlabs.no/certificate-not-after";
    public static final String LAST_ROTATED_AT_ANNOTATION = "kafkarator.fintlabs.no/last-rotated-at";
    private final AivenService aivenService;
    private final AivenProperties aivenProperties;
    private final CertificateMetricsService certificateMetricsService;
    private final KeyStoreService keyStoreService;
    private final TrustStoreService trustStoreService;

    public CertificateSecretDependentResource(FlaisWorkflow<KafkaUserAndAclCrd, KafkaUserAndAclSpec> workflow, KubernetesClient kubernetesClient, KafkaSecretDependentResource kafkaSecretDependentResource, KafkaUserAndAclDependentResource kafkaUserAndAclDependentResource, AivenService aivenService, AivenProperties aivenProperties, CertificateMetricsService certificateMetricsService, KeyStoreService keyStoreService, CertificateSecretDiscriminator discriminator, TrustStoreService trustStoreService) {

        super(Secret.class, workflow, kubernetesClient);
        this.aivenService = aivenService;
        this.aivenProperties = aivenProperties;
        this.certificateMetricsService = certificateMetricsService;
        this.keyStoreService = keyStoreService;
        this.trustStoreService = trustStoreService;
        dependsOn(kafkaSecretDependentResource, kafkaUserAndAclDependentResource);
        setResourceDiscriminator(discriminator);
        configureWith(new KubernetesDependentResourceConfig<Secret>().setLabelSelector("app.kubernetes.io/managed-by=kafkarator"));
    }

    @Override
    protected Secret desired(KafkaUserAndAclCrd resource, Context<KafkaUserAndAclCrd> context) {
        log.debug("Desired certificate secret for {}", resource.getMetadata().getName());
        Sample timerSample = certificateMetricsService.startReconcile();
        boolean rotationAttempted = false;

        try {
            KafkaUserAndAcl kafkaUserAndAcl = context.getSecondaryResource(KafkaUserAndAcl.class).orElseThrow();
            Secret kafkaSecret = context.getSecondaryResources(Secret.class)
                    .stream()
                    .filter(secret -> secret.getMetadata().getName().equals(KafkaSecretDependentResource.getResourceName(resource)))
                    .findFirst()
                    .orElseThrow();

            Optional<Secret> thisSecret = context.getSecondaryResources(Secret.class)
                    .stream()
                    .filter(secret -> secret.getMetadata().getName().equals(getResourceName(resource)))
                    .findFirst();

            String keyStorePassword = decode(kafkaSecret.getData().get("spring.kafka.ssl.key-store-password"));
            String trustStorePassword = decode(kafkaSecret.getData().get("spring.kafka.ssl.trust-store-password"));

            String existingKeyStore = thisSecret.map(secret -> secret.getData().get("client.keystore.p12")).orElse(null);
            KeyStoreService.KeyStoreInspection keyStoreInspection = existingKeyStore == null ? KeyStoreService.KeyStoreInspection.invalid("No existing key store") : keyStoreService.inspectKeyStore(existingKeyStore, keyStorePassword);
            certificateMetricsService.recordInspection(resource, aivenProperties.getService(), keyStoreInspection, aivenProperties.getCertificateRotationThreshold());
            boolean rotateCredentials = keyStoreInspection.needsRotation(aivenProperties.getCertificateRotationThreshold());
            rotationAttempted = rotateCredentials;

            if (rotateCredentials) {
                log.info("Rotating Kafka client certificate for {} because {}", resource.getMetadata().getName(), rotationReason(existingKeyStore, keyStoreInspection));
            }

            String keyStore = rotateCredentials ? keyStoreService.createKeyStoreAndGetAsBase64(kafkaUserAndAcl.getUser().getAccessCert(), kafkaUserAndAcl.getUser().getAccessKey(), aivenService.getCa(), keyStorePassword.toCharArray()) : existingKeyStore;

            String trustStore = rotateCredentials ? trustStoreService.createTrustStoreAndGetAsBase64(aivenService.getCa(), trustStorePassword.toCharArray()) : thisSecret.map(ts -> ts.getData().get("client.truststore.jks")).map(ts -> trustStoreService.verifyTrustStore(ts, trustStorePassword)).orElseGet(() -> {
                log.info("No trust store available. Creating a new one!");

                return trustStoreService.createTrustStoreAndGetAsBase64(aivenService.getCa(), trustStorePassword.toCharArray());
            });

            HashMap<String, String> labels = new HashMap<>(resource.getMetadata().getLabels());
            labels.put("app.kubernetes.io/managed-by", "kafkarator");
            Map<String, String> annotations = new HashMap<>(thisSecret.map(Secret::getMetadata).map(metadata -> Optional.ofNullable(metadata.getAnnotations()).orElse(Collections.emptyMap())).orElse(Collections.emptyMap()));
            KeyStoreService.KeyStoreInspection resultingKeyStoreInspection = keyStoreService.inspectKeyStore(keyStore, keyStorePassword);
            certificateMetricsService.updateResourceState(resource, aivenProperties.getService(), resultingKeyStoreInspection, aivenProperties.getCertificateRotationThreshold());
            if (resultingKeyStoreInspection.notAfter() != null) {
                annotations.put(CERTIFICATE_NOT_AFTER_ANNOTATION, resultingKeyStoreInspection.notAfter().toString());
            }
            if (rotateCredentials) {
                annotations.put(LAST_ROTATED_AT_ANNOTATION, Instant.now().toString());
                certificateMetricsService.recordRotation(resource, aivenProperties.getService(), resultingKeyStoreInspection.readable(), rotationReason(existingKeyStore, keyStoreInspection));
            }


            return new SecretBuilder().withNewMetadata().withName(getResourceName(resource)).withNamespace(resource.getMetadata().getNamespace()).withLabels(labels).withAnnotations(annotations).endMetadata().withType("Opaque").addToData("client.keystore.p12", keyStore).addToData("client.truststore.jks", trustStore).build();
        } catch (RuntimeException e) {
            if (rotationAttempted) {
                certificateMetricsService.recordRotation(resource, aivenProperties.getService(), false, "reconcile-failure");
            }
            throw e;
        } finally {
            certificateMetricsService.recordReconcile(resource, aivenProperties.getService(), timerSample);
        }
    }

    public static String getResourceName(KafkaUserAndAclCrd resource) {
        return resource.getMetadata().getName() + NAME_SUFFIX;
    }

    @Override
    public Matcher.Result<Secret> match(Secret actualResource, KafkaUserAndAclCrd primary, Context<KafkaUserAndAclCrd> context) {
        return super.match(actualResource, primary, context);
    }

    private String rotationReason(String existingKeyStore, KeyStoreService.KeyStoreInspection inspection) {
        if (existingKeyStore == null) {
            return "no existing key store is present";
        }
        if (!inspection.readable()) {
            return "the existing key store is unreadable";
        }
        return "the certificate expires before the configured rotation threshold";
    }
}
