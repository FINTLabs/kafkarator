package no.fintlabs.aivenerator.operator;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("fintlabs.no")
@Version("v1alpha1")
@Kind("AivenUser")
public class AivenUserCrd extends CustomResource<AivenUserSpec, AivenUserStatus> implements Namespaced {
    @Override
    protected AivenUserStatus initStatus() {
        return new AivenUserStatus();
    }
}

