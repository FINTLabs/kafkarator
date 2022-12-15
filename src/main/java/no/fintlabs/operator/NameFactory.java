package no.fintlabs.operator;

import io.fabric8.kubernetes.api.model.HasMetadata;

public class NameFactory {

    public static String nameFromMetadata(HasMetadata metadata) {
        return String.format("%s_%s_%s",
                metadata.getMetadata().getLabels().get("fintlabs.no/org-id").replace(".", "-"),
                metadata.getMetadata().getLabels().get("fintlabs.no/team"),
                metadata.getMetadata().getName()
        );
    }
}
