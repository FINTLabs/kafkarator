package no.fintlabs;

import io.fabric8.kubernetes.client.KubernetesClient;
import no.fintlabs.aiven.AivenService;
import no.fintlabs.operator.CertificateSecretDependentResource;
import no.fintlabs.operator.KafkaSecretDependentResource;
import no.fintlabs.operator.KafkaUserAclReconciler;
import no.fintlabs.operator.KafkaUserAndAclDependentResource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
        classes = Application.class,
        properties = {
                "spring.main.web-application-type=none",
                "spring.autoconfigure.exclude=io.javaoperatorsdk.operator.springboot.starter.OperatorAutoConfiguration",
                "fint.aiven.service=test-service",
                "fint.aiven.kafka-bootstrap-servers=localhost:9092"
        }
)
class ApplicationContextTest {

    @MockBean
    private KubernetesClient kubernetesClient;

    @MockBean
    private AivenService aivenService;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoadsCriticalOperatorBeans() {
        assertNotNull(applicationContext.getBean(KafkaUserAclReconciler.class));
        assertNotNull(applicationContext.getBean(KafkaUserAndAclDependentResource.class));
        assertNotNull(applicationContext.getBean(KafkaSecretDependentResource.class));
        assertNotNull(applicationContext.getBean(CertificateSecretDependentResource.class));
    }
}
