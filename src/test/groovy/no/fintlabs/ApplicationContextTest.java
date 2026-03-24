package no.fintlabs;

import io.fabric8.kubernetes.client.KubernetesClient;
import no.fintlabs.aiven.AivenProperties;
import no.fintlabs.aiven.AivenService;
import no.fintlabs.keystore.KeyStoreService;
import no.fintlabs.keystore.TrustStoreService;
import no.fintlabs.operator.CertificateSecretDependentResource;
import no.fintlabs.operator.CertificateMetricsService;
import no.fintlabs.operator.CertificateSecretDiscriminator;
import no.fintlabs.operator.KafkaSecretDependentResource;
import no.fintlabs.operator.KafkaSecretDiscriminator;
import no.fintlabs.operator.KafkaUserAclReconciler;
import no.fintlabs.operator.KafkaUserAndAclDependentResource;
import no.fintlabs.operator.KafkaUserAndAclWorkflow;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ApplicationContextTest.TestConfig.class)
@TestPropertySource(properties = {
        "fint.aiven.service=test-service",
        "fint.aiven.kafka-bootstrap-servers=localhost:9092"
})
class ApplicationContextTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoadsCriticalOperatorBeans() {
        assertNotNull(applicationContext.getBean(KafkaUserAclReconciler.class));
        assertNotNull(applicationContext.getBean(KafkaUserAndAclDependentResource.class));
        assertNotNull(applicationContext.getBean(KafkaSecretDependentResource.class));
        assertNotNull(applicationContext.getBean(CertificateSecretDependentResource.class));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AivenProperties.class)
    @Import({
            KafkaUserAclReconciler.class,
            KafkaUserAndAclWorkflow.class,
            KafkaUserAndAclDependentResource.class,
            KafkaSecretDependentResource.class,
            CertificateSecretDependentResource.class,
            CertificateMetricsService.class,
            KafkaSecretDiscriminator.class,
            CertificateSecretDiscriminator.class,
            KeyStoreService.class,
            TrustStoreService.class
    })
    static class TestConfig {

        @Bean
        KubernetesClient kubernetesClient() {
            return mock(KubernetesClient.class);
        }

        @Bean
        AivenService aivenService() {
            return mock(AivenService.class);
        }

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
