package no.fintlabs.operator;

import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Deleter;
import io.javaoperatorsdk.operator.api.reconciler.dependent.EventSourceProvider;
import lombok.extern.slf4j.Slf4j;
import no.fintlabs.FlaisReconiler;
import no.fintlabs.FlaisWorkflow;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@ControllerConfiguration
public class AivenKafkaAclReconciler extends FlaisReconiler<AivenKafkaAclCrd, AivenKafkaAclSpec> {
    public AivenKafkaAclReconciler(FlaisWorkflow<AivenKafkaAclCrd, AivenKafkaAclSpec> workflow, List<? extends EventSourceProvider<AivenKafkaAclCrd>> eventSourceProviders, List<? extends Deleter<AivenKafkaAclCrd>> deleters) {
        super(workflow, eventSourceProviders, deleters);
    }
}
