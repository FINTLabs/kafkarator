package no.fintlabs.operator;

import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Deleter;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.FlaisReconiler;
import no.fintlabs.FlaisWorkflow;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@ControllerConfiguration
public class KafkaUserAclReconciler extends FlaisReconiler<KafkaUserAndAclCrd, KafkaUserAndAclSpec> {
    public KafkaUserAclReconciler(FlaisWorkflow<KafkaUserAndAclCrd,
            KafkaUserAndAclSpec> workflow,
                                  List<? extends DependentResource<?, KafkaUserAndAclCrd>> eventSourceProviders,

                                  List<? extends Deleter<KafkaUserAndAclCrd>> deleters) {
        super(workflow, eventSourceProviders, deleters);
    }
}
