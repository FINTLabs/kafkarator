package no.fintlabs.aivenerator.operator;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import no.fintlabs.FlaisCrd;
import no.fintlabs.FlaisStatus;

@Group("fintlabs.no")
@Version("v1alpha1")
@Kind("AivenKafkaAcl")
public class AivenKafkaAclCrd extends FlaisCrd<AivenKafkaAclSpec>/*CustomResource<AivenKafkaAclSpec, FlaisStatus>*/ implements Namespaced {
    @Override
    protected FlaisStatus initStatus() {
        return new FlaisStatus();
    }
}

